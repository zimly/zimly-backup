package io.zeitmaschine.zimzync

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.*
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncModel(private val dao: RemoteDao, remoteId: Int, application: Application) :
    AndroidViewModel(application) {

    // Todo: https://luisramos.dev/testing-your-android-viewmodel
    companion object {
        val TAG: String? = SyncModel::class.simpleName
    }

    private val contentResolver by lazy { application.contentResolver }
    private val internal: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    var uiState: StateFlow<UiState> = internal.asStateFlow()
    private val mediaRepo: MediaRepository = ResolverBasedRepository(contentResolver)

    private lateinit var s3Repo: S3Repository
    private lateinit var syncService: SyncService

    init {
        viewModelScope.launch {
            val remote = dao.loadById(remoteId)
            internal.update {
                it.copy(name = remote.name, url = remote.url, bucket = remote.bucket, key = remote.key, secret = remote.secret)
            }
            try {
                s3Repo = MinioRepository(remote.url, remote.key, remote.secret, remote.bucket)
            } catch (e: Exception) {
                // TODO: Exception handling in a lateinit block, inside a viewModelFactory, inside a
                //  NavHost Composable? Some global error handling?
                Log.e(TAG, "Failed to initialize minio repo: ${e.message}", e)
            }

            syncService = SyncServiceImpl(s3Repo, mediaRepo)
            createDiff()
        }
    }

    suspend fun createDiff() {
        // Display result of the minio request to the user
        when (val result = syncService.diff()) {
            is Result.Success<Diff> -> {
                internal.update {
                    it.copy(
                        remoteCount = result.data.remotes.size,
                        localCount = result.data.locals.size,
                        diff = result.data
                    )
                }
            }
            is Result.Error -> Log.e(
                TAG,
                "Failed to create log",
                result.exception
            )// Show error in UI
        }
    }

    fun sync() {
        // Display result of the minio request to the user
        val data = workDataOf(
            SyncConstants.S3_URL to uiState.value.url,
            SyncConstants.S3_KEY to uiState.value.key,
            SyncConstants.S3_SECRET to uiState.value.secret,
            SyncConstants.S3_BUCKET to uiState.value.bucket
        )
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(getApplication<Application>().applicationContext).enqueue(syncRequest)
    }

    data class UiState(
        var name: String = "",
        var url: String = "",
        var key: String = "",
        var secret: String = "",
        var bucket: String = "",

        var localCount: Int = 0,
        var remoteCount: Int = 0,
        var diff: Diff = Diff(emptyList(), emptyList())
    )

}

@Composable
fun SyncRemote(
    dao: RemoteDao,
    remoteId: Int,
    application: Application,
    edit: (Int) -> Unit,
    viewModel: SyncModel = viewModel(factory = viewModelFactory {
        initializer {
            SyncModel(dao, remoteId, application)
        }
    }),
) {
    val state = viewModel.uiState.collectAsState()

    SyncCompose(
        state,
        sync = { viewModel.viewModelScope.launch { viewModel.sync() } },
        edit = { edit(remoteId) })
}

@Composable
private fun SyncCompose(
    state: State<SyncModel.UiState>,
    sync: () -> Unit,
    edit: () -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(all = 16.dp)
    ) {
        Text(state.value.name)
        Text(state.value.url)
        Text(state.value.bucket)
        Text(state.value.key)
        Text(state.value.secret)
        Text("${state.value.remoteCount}")
        Text("${state.value.localCount}")
        Button(
            modifier = Modifier.align(Alignment.End),
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


@Preview(showBackground = true)
@Composable
fun SyncPreview() {

    val uiState = SyncModel.UiState(
        name = "zeitmaschine.io",
        url = "http://10.0.2.2:9000",
        key = "test",
        secret = "testtest",
        bucket = "test-bucket",
        diff = Diff(emptyList(), emptyList()),
    )
    val internal: MutableStateFlow<SyncModel.UiState> = MutableStateFlow(uiState)


    ZimzyncTheme {
        SyncCompose(
            state = internal.collectAsState(), sync = {}, edit = {}
        )
    }
}
