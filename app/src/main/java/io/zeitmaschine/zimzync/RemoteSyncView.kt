package io.zeitmaschine.zimzync

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.workDataOf
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import io.zeitmaschine.zimzync.ui.theme.containerBackground
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SyncModel(private val dao: RemoteDao, private val remoteId: Int, application: Application) :
    AndroidViewModel(application) {

    // Todo: https://luisramos.dev/testing-your-android-viewmodel
    companion object {
        val TAG: String? = SyncModel::class.simpleName
    }

    // This identifier is used to identify already running sync instances and prevent simultaneous
    // sync-executions.
    private var uniqueWorkIdentifier = "sync_${remoteId}"
    private val workManager =
        WorkManager.getInstance(getApplication<Application>().applicationContext)

    private val contentResolver by lazy { application.contentResolver }

    private val _diff: MutableStateFlow<Diff> = MutableStateFlow(Diff.EMPTY)
    val diff: StateFlow<Diff> = _diff.asStateFlow()

    var remoteState: StateFlow<RemoteState> = snapshotFlow { remoteId }
        .map { dao.loadById(it) }
        .map {
            RemoteState(
                name = it.name,
                url = it.url,
                bucket = it.bucket,
                key = it.key,
                secret = it.secret,
                folder = it.folder
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RemoteState(),
        )

    // Flow created when starting the sync
    private val _startedSyncId: MutableStateFlow<UUID?> = MutableStateFlow(null)

    // Flow for already running sync jobs
    private val _runningSyncId: Flow<UUID?> = flow { loadSyncState() }

    // merge the flows for and observe
    var progressState: StateFlow<Progress> =
        merge(_runningSyncId, _startedSyncId)
            .onEach { Log.i(TAG, "syncId: $it") }
            .filterNotNull()
            .flatMapLatest { observeSyncProgress(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Progress(),
            )


    // mutable error flow, for manually triggered errors
    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)

    // Merge errors from progress and manually triggered errors into one observable for the UI
    // TODO there's still a corner case where _error doesn't emit?
    val error: StateFlow<String?> = merge(_error, progressState.map { it.error })
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val mediaRepo: MediaRepository = ResolverBasedRepository(contentResolver)

    private lateinit var contentBuckets: Set<String>

    init {
        viewModelScope.launch {
            try {
                contentBuckets = mediaRepo.getBuckets().keys
            } catch (e: Exception) {
                // TODO: Exception handling in a lateinit block, inside a viewModelFactory, inside a
                //  NavHost Composable? Some global error handling?
                Log.e(TAG, "Failed to initialize minio repo: ${e.message}", e)
                _error.update { e.message ?: "Unknown error." }
            }
        }
    }

    suspend fun createDiff() {

        val remote = dao.loadById(remoteId)
        val s3Repo = MinioRepository(remote.url, remote.key, remote.secret, remote.bucket)
        val syncService = SyncServiceImpl(s3Repo, mediaRepo)
        // Display result of the minio request to the user
        when (val result = syncService.diff(setOf(remote.folder))) {
            is Result.Success<Diff> -> {
                _diff.update { result.data }
            }

            is Result.Error -> {
                Log.e(TAG, "Failed to create diff.", result.exception)

                _error.update { result.exception.message ?: "Unknown error." }
            }
        }
    }

    // TODO https://code.luasoftware.com/tutorials/android/jetpack-compose-load-data
    private fun loadSyncState(): UUID? {
        val query = WorkQuery.Builder
            .fromUniqueWorkNames(listOf(uniqueWorkIdentifier))
            .addStates(listOf(WorkInfo.State.RUNNING))
            .build()
        val running = workManager.getWorkInfos(query).get()

        return when (running.size) {
            0 -> null // empty
            1 -> running.first().id
            else -> throw Error("More than one unique sync job in progress. This should not happen.")
        }
    }

    private fun observeSyncProgress(id: UUID): Flow<Progress> {
        return workManager.getWorkInfoByIdFlow(id)
            .filterNotNull()
            .map { workInfo ->

            val progressState = Progress()

            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED, WorkInfo.State.ENQUEUED -> {
                    val output = workInfo.outputData
                    progressState.syncCount = output.getInt(SYNC_COUNT, 0)
                    progressState.syncBytes = output.getLong(SYNC_BYTES, 0)
                    progressState.inProgress = false
                }

                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress
                    progressState.syncCount = progress.getInt(SYNC_COUNT, 0)
                    progressState.syncBytes = progress.getLong(SYNC_BYTES, 0)
                    progressState.inProgress = true
                }

                WorkInfo.State.FAILED -> {
                    val output = workInfo.outputData
                    progressState.syncCount = output.getInt(SYNC_COUNT, 0)
                    progressState.syncBytes = output.getLong(SYNC_BYTES, 0)
                    progressState.error = output.getString(SYNC_ERROR) ?: "Unknown error."
                    progressState.inProgress = false
                }

                else -> {}
            }

            if (progressState.syncBytes > 0) {
                progressState.percentage =
                    progressState.syncBytes.toFloat() / diff.value.size
            }
            progressState
        }
    }

    // TODO check error handling!
    suspend fun sync(): UUID {
        val remote = dao.loadById(remoteId)
        val data = workDataOf(
            SyncConstants.S3_URL to remote.url,
            SyncConstants.S3_KEY to remote.key,
            SyncConstants.S3_SECRET to remote.secret,
            SyncConstants.S3_BUCKET to remote.bucket,
            SyncConstants.DEVICE_FOLDER to arrayOf(remote.folder)
        )
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(uniqueWorkIdentifier, ExistingWorkPolicy.KEEP, syncRequest)
        _startedSyncId.update { syncRequest.id }

        return syncRequest.id
    }

    fun clearError() {
        _error.update { null }
    }

    data class RemoteState(
        var name: String = "",
        var url: String = "",
        var key: String = "",
        var secret: String = "",
        var bucket: String = "",
        var folder: String = "",
    )

    data class Progress(
        var percentage: Float = 0.0f,
        var syncBytes: Long = 0,
        var syncCount: Int = 0,
        var inProgress: Boolean = false,
        var error: String = "",
    )
}

@Composable
fun SyncRemote(
    dao: RemoteDao,
    remoteId: Int,
    application: Application,
    edit: (Int) -> Unit,
    back: () -> Unit,
    viewModel: SyncModel = viewModel(factory = viewModelFactory {
        initializer {
            SyncModel(dao, remoteId, application)
        }
    }),
) {

    val remote by viewModel.remoteState.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val diff by viewModel.diff.collectAsStateWithLifecycle()
    val progress by viewModel.progressState.collectAsStateWithLifecycle()

    // want to go nuts?
    // https://afigaliyev.medium.com/snackbar-state-management-best-practices-for-jetpack-compose-1a5963d86d98
    val snackbarState = remember { SnackbarHostState() }

    SyncCompose(
        remote,
        error,
        diff = diff,
        progress,
        snackbarState,
        sync = {
            viewModel.viewModelScope.launch {
                viewModel.sync()
            }
        },
        createDiff = { viewModel.viewModelScope.launch { viewModel.createDiff() } },
        edit = { edit(remoteId) },
        back,
        clearError = { viewModel.clearError() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncCompose(
    remote: SyncModel.RemoteState,
    error: String?,
    diff: Diff,
    progress: SyncModel.Progress,
    snackbarState: SnackbarHostState,
    sync: () -> Unit,
    createDiff: () -> Unit,
    edit: () -> Unit,
    back: () -> Unit,
    clearError: () -> Unit
) {
    // If the UI state contains an error, show snackbar
    if (!error.isNullOrEmpty()) {
        LaunchedEffect(snackbarState) {
            val result = snackbarState.showSnackbar(
                message = error,
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite
            )
            when (result) {
                SnackbarResult.Dismissed -> clearError()
                SnackbarResult.ActionPerformed -> clearError()
            }

        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        remote.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { back() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { edit() }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Remote"
                        )
                    }

                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }) { innerPadding ->
        Column(
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = containerBackground()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Outlined.CloudUpload,
                        "Remote",
                        modifier = Modifier.padding(top = 8.dp, end = 8.dp)
                    )
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "URL", textAlign = TextAlign.Left)
                        Text(remote.url, textAlign = TextAlign.Right)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Bucket", textAlign = TextAlign.Left)
                        Text(remote.bucket)
                    }
                }
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = containerBackground(),
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Outlined.Photo,
                        "Media",
                        modifier = Modifier.padding(top = 8.dp, end = 8.dp)
                    )
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Remotes")
                        Text(text = "${diff.remotes.size}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Folder")
                        Text(text = remote.folder)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Locales")
                        Text(text = "${diff.locals.size}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Diffs / #")
                        Text(text = "${diff.diff.size} / ${diff.size}")
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = containerBackground(),
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Outlined.Upload,
                        "Progress",
                        modifier = Modifier.padding(top = 8.dp, end = 8.dp)
                    )
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Uploaded")
                        Text(text = "${progress.syncCount}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Uploaded KB")
                        Text(text = "${progress.syncBytes}")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress.percentage },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Column(
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = createDiff,
                        modifier = Modifier.weight(1f),
                        colors = ButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                    ) {
                        Text(text = "Refresh")
                    }
                    Button(
                        onClick = sync,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Upload")
                    }
                }

            }


        }
    }
}


@Preview(showBackground = true)
@Composable
fun SyncPreview() {

    val remote = SyncModel.RemoteState(
        name = "zeitmaschine.io",
        url = "http://10.0.2.2:9000",
        key = "test",
        secret = "testtest",
        bucket = "test-bucket",
        folder = "Camera"
    )
    val progressState = SyncModel.Progress()
    val snackbarState = remember { SnackbarHostState() }


    ZimzyncTheme {
        SyncCompose(
            remote = remote,
            error = null,
            diff = Diff.EMPTY,
            progressState,
            sync = {},
            createDiff = {},
            edit = {},
            back = {},
            snackbarState = snackbarState,
            clearError = {}
        )
    }
}
