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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

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
    private val internal: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    var uiState: StateFlow<UiState> = internal.asStateFlow()

    private val mediaRepo: MediaRepository = ResolverBasedRepository(contentResolver)

    private lateinit var s3Repo: S3Repository
    private lateinit var syncService: SyncService
    private lateinit var contentBuckets: Set<String>

    init {
        viewModelScope.launch {
            val remote = dao.loadById(remoteId)
            internal.update {
                it.copy(
                    name = remote.name,
                    url = remote.url,
                    bucket = remote.bucket,
                    key = remote.key,
                    secret = remote.secret,
                    folder = remote.folder,
                )
            }
            try {
                s3Repo = MinioRepository(remote.url, remote.key, remote.secret, remote.bucket)
                syncService = SyncServiceImpl(s3Repo, mediaRepo)
                contentBuckets = mediaRepo.getBuckets().keys
            } catch (e: Exception) {
                // TODO: Exception handling in a lateinit block, inside a viewModelFactory, inside a
                //  NavHost Composable? Some global error handling?
                Log.e(TAG, "Failed to initialize minio repo: ${e.message}", e)
                internal.update {
                    it.copy(
                        error = e.message ?: "Unknown error.",
                    )
                }
            }
        }
    }

    suspend fun createDiff() {

        // Display result of the minio request to the user
        when (val result = syncService.diff(setOf(uiState.value.folder))) {
            is Result.Success<Diff> -> {
                internal.update {
                    it.copy(
                        diff = result.data
                    )
                }
            }

            is Result.Error -> {
                Log.e(TAG, "Failed to create diff.", result.exception)

                internal.update {
                    it.copy(
                        error = result.exception.message ?: "Unknown error.",
                    )
                }
            }
        }
    }

    fun loadSyncState(lifeCycleOwner: LifecycleOwner) {
        val query = WorkQuery.Builder
            .fromUniqueWorkNames(listOf(uniqueWorkIdentifier))
            .addStates(listOf(WorkInfo.State.RUNNING))
            .build()
        val running = workManager.getWorkInfos(query).get()

        when (running.size) {
            0 -> return
            1 -> observeSyncProgress(lifeCycleOwner, running.first().id)
            else -> throw Error("More than one unique sync job in progress. This should not happen.")
        }
    }

    fun observeSyncProgress(owner: LifecycleOwner, id: UUID) {
        workManager.getWorkInfoByIdLiveData(id).observe(owner) { workInfo: WorkInfo ->

            var inProgress = false
            var syncProgress = 0.0F
            var syncCount = 0
            var syncBytes = 0L
            var error = ""

            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED, WorkInfo.State.ENQUEUED -> {
                    val output = workInfo.outputData
                    syncCount = output.getInt(SYNC_COUNT, 0)
                    syncBytes = output.getLong(SYNC_BYTES, 0)
                    inProgress = false
                }

                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress
                    syncCount = progress.getInt(SYNC_COUNT, 0)
                    syncBytes = progress.getLong(SYNC_BYTES, 0)
                    inProgress = true
                }

                WorkInfo.State.FAILED -> {
                    val output = workInfo.outputData
                    syncCount = output.getInt(SYNC_COUNT, 0)
                    syncBytes = output.getLong(SYNC_BYTES, 0)
                    error = output.getString(SYNC_ERROR) ?: "Unknown error."
                    inProgress = false
                }

                else -> {}
            }


            if (syncBytes > 0) {
                syncProgress = syncBytes.toFloat() / uiState.value.diff.size
            }
            internal.update {
                it.copy(
                    inProgress = inProgress,
                    progress = syncProgress,
                    syncBytes = syncBytes,
                    syncCount = syncCount,
                    error = error
                )
            }
        }
    }

    fun sync(): UUID {
        val data = workDataOf(
            SyncConstants.S3_URL to uiState.value.url,
            SyncConstants.S3_KEY to uiState.value.key,
            SyncConstants.S3_SECRET to uiState.value.secret,
            SyncConstants.S3_BUCKET to uiState.value.bucket,
            SyncConstants.DEVICE_FOLDER to arrayOf(uiState.value.folder)
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
        return syncRequest.id
    }

    fun clearError() {
        internal.update { it.copy(error = "") }
    }


    data class UiState(
        var name: String = "",
        var url: String = "",
        var key: String = "",
        var secret: String = "",
        var bucket: String = "",

        var folder: String = "",

        var diff: Diff = Diff.EMPTY,
        var progress: Float = 0.0f,
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
    lifeCycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val state = viewModel.uiState.collectAsState()

    // want to go nuts?
    // https://afigaliyev.medium.com/snackbar-state-management-best-practices-for-jetpack-compose-1a5963d86d98
    val snackbarState = remember { SnackbarHostState() }

    viewModel.loadSyncState(lifeCycleOwner)
    SyncCompose(
        state,
        snackbarState,
        sync = {
            viewModel.viewModelScope.launch {
                val id = viewModel.sync()
                viewModel.observeSyncProgress(lifeCycleOwner, id)
            }
        },
        diff = { viewModel.viewModelScope.launch { viewModel.createDiff() } },
        edit = { edit(remoteId) },
        back,
        clearError = { viewModel.clearError() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncCompose(
    state: State<SyncModel.UiState>,
    snackbarState: SnackbarHostState,
    sync: () -> Unit,
    diff: () -> Unit,
    edit: () -> Unit,
    back: () -> Unit,
    clearError: () -> Unit
) {
    // If the UI state contains an error, show snackbar
    if (state.value.error.isNotEmpty()) {
        LaunchedEffect(snackbarState) {
            val result = snackbarState.showSnackbar(
                message = state.value.error,
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
                        state.value.name,
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
                        Text(state.value.url, textAlign = TextAlign.Right)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Bucket", textAlign = TextAlign.Left)
                        Text(state.value.bucket)
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
                        Text(text = "${state.value.diff.remotes.size}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Folder")
                        Text(text = state.value.folder)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Locales")
                        Text(text = "${state.value.diff.locals.size}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Diffs / #")
                        Text(text = "${state.value.diff.diff.size} / ${state.value.diff.size}")
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
                        Text(text = "${state.value.syncCount}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Uploaded KB")
                        Text(text = "${state.value.syncBytes}")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { state.value.progress },
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
                        onClick = diff,
                        modifier = Modifier.weight(1f),
                        colors = ButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface),
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

    val uiState = SyncModel.UiState(
        name = "zeitmaschine.io",
        url = "http://10.0.2.2:9000",
        key = "test",
        secret = "testtest",
        bucket = "test-bucket",
        diff = Diff.EMPTY,
        inProgress = false,
    )
    val internal: MutableStateFlow<SyncModel.UiState> = MutableStateFlow(uiState)
    val snackbarState = remember { SnackbarHostState() }


    ZimzyncTheme {
        SyncCompose(
            state = internal.collectAsState(),
            sync = {},
            diff = {},
            edit = {},
            back = {},
            snackbarState = snackbarState,
            clearError = {}
        )
    }
}
