package io.zeitmaschine.zimzync

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SyncModel(private val dao: RemoteDao, remoteId: Int, application: Application) :
    AndroidViewModel(application) {

    companion object {
        val TAG: String? = SyncModel::class.simpleName
    }

    private val contentResolver by lazy { application.contentResolver }

    private var remote: Remote = Remote(null, "", "", "", "", "")
    private var photos: MediaRepository = MediaRepository(contentResolver)
    private lateinit var minio: MinioRepository
    var uiState: MutableStateFlow<Remote> = MutableStateFlow(remote)

    init {
        viewModelScope.launch {
            remote = dao.loadById(remoteId)
            uiState.value = remote
            minio = MinioRepository(remote.url, remote.key, remote.secret, remote.bucket)
        }
    }

    suspend fun sync() {
        // Display result of the minio request to the user
        when (val result = minio.listObjects()) {
            is Result.Success<Boolean> -> Log.d(TAG, "win")// Happy path
            else -> Log.e(TAG, "FML")// Show error in UI
        }
    }

    suspend fun photos() {
        // Display result of the minio request to the user
        when (val result = photos.getPhotos()) {
            is Result.Success<List<String>> -> result.data.forEach { photo -> Log.i(TAG, photo) }
            else -> Log.e(TAG, "FML")// Show error in UI
        }
    }

}


@Composable
fun SyncRemote(
    dao: RemoteDao,
    remoteId: Int,
    application: Application,
    viewModel: SyncModel = viewModel(factory = viewModelFactory {
        initializer {
            SyncModel(dao, remoteId, application)
        }
    }),
) {
    val remote = viewModel.uiState.collectAsState()

    SyncCompose(
        remote = remote.value,
        sync = { viewModel.viewModelScope.launch { viewModel.sync() } },
        photos = { viewModel.viewModelScope.launch { viewModel.photos() } })
}

@Composable
private fun SyncCompose(
    remote: Remote,
    sync: () -> Unit,
    photos: () -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(all = 16.dp)
    ) {
        Text(remote.name)
        Text(remote.url)
        Text(remote.bucket)
        Text(remote.key)
        Text(remote.secret)
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
                photos()
            }
        )
        {
            Text(text = "Photos")
        }
        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = {
                photos()
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
    val remote = Remote(
        uid = 123,
        name = "zeitmaschine.io",
        url = "http://10.0.2.2:9000",
        key = "test",
        secret = "testtest",
        bucket = "testbucket"
    )


    ZimzyncTheme {
        SyncCompose(
            remote, sync = {}, photos = {}
        )
    }
}
