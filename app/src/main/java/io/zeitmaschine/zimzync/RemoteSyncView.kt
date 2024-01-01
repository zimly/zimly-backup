package io.zeitmaschine.zimzync

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    fun loadSyncState(owner: LifecycleOwner) {
        val query = WorkQuery.Builder
            .fromUniqueWorkNames(listOf(uniqueWorkIdentifier))
            .addStates(listOf(WorkInfo.State.RUNNING))
            .build()

        workManager.getWorkInfosLiveData(query)
            .observe(owner, Observer { workInfos: List<WorkInfo> ->

                if (workInfos.size > 1) throw Exception("More than one sync running. This should never happen.")

                if (workInfos.size == 1) {
                    val workInfo = workInfos.first()
                    var synced = 0
                    var error = ""
                    var inProgress = true
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED, WorkInfo.State.ENQUEUED -> {
                            val output = workInfo.outputData
                            synced = output.getInt("synced", 0)
                            inProgress = false
                        }

                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress
                            synced = progress.getInt("synced", 0)
                            inProgress = true
                        }

                        WorkInfo.State.FAILED -> {
                            val progress = workInfo.progress
                            synced = progress.getInt("synced", 0)
                            error = progress.getString("error")!!
                            inProgress = false
                        }

                        else -> {}
                    }

                    if (synced > 0) {
                        var progress: Float = synced.toFloat() / uiState.value.diff.diff.size
                        internal.update {
                            it.copy(
                                progress = progress,
                                inProgress = inProgress
                            )
                        }
                    }
                    if (error.isNotEmpty()) {
                        internal.update {

                            it.copy(
                                error = error,
                                inProgress = inProgress
                            )
                        }
                    }
                }

            })
    }

    fun sync() {
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
                viewModel.sync()
                viewModel.loadSyncState(lifeCycleOwner)
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
                colors = topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
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
                            tint = MaterialTheme.colorScheme.onPrimary,
                            contentDescription = "Localized description"
                        )
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }) { innerPadding ->
        Column(
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(state.value.name)
            Text(state.value.url)
            Text(state.value.bucket)
            Text(state.value.key)
            Text(state.value.secret)
            Text(state.value.folder)
            Text("Remotes: ${state.value.diff.remotes.size}")
            Text("Locales: ${state.value.diff.locals.size}")
            Text("#Diffs: ${state.value.diff.diff.size}")
            Text("Diff Size: ${state.value.diff.size}")
            Text("Progress: ${state.value.progress}")
            LinearProgressIndicator(progress = state.value.progress)
            Button(
                modifier = Modifier.align(Alignment.End),
                enabled = !state.value.inProgress,
                onClick = {
                    diff()
                }
            )
            {
                Text(text = "Diff")
            }
            Button(
                modifier = Modifier.align(Alignment.End),
                enabled = !state.value.inProgress,
                onClick = {
                    sync()
                }
            )
            {
                Text(text = "Sync")
            }
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    edit()
                }
            )
            {
                Text(text = "Edit")
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
