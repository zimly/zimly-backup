package io.zeitmaschine.zimzync

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SyncModel(private val dao: RemoteDao, remoteId: Int) : ViewModel() {

    companion object {
        val TAG: String? = SyncModel::class.simpleName
    }

    private var remote: Remote = Remote(null, "", "", "", "")
    lateinit var minio: MinioRepository
    var uiState: StateFlow<Remote> = MutableStateFlow(remote)

    init {
        viewModelScope.launch {
            remote = dao.loadById(remoteId)
            uiState = MutableStateFlow(remote)
            minio = MinioRepository(remote.url, remote.key, remote.secret, "test")
        }
    }

    suspend fun sync() {
        // Display result of the minio request to the user
        when (val result = minio.listBuckets()) {
            is Result.Success<List<String>> -> Log.d(TAG, result.data.first())// Happy path
            else -> Log.e(TAG, "FML")// Show error in UI
        }
    }
}


@Composable
fun SyncRemote(
    dao: RemoteDao,
    remoteId: Int,
    viewModel: SyncModel = viewModel(factory = viewModelFactory {
        initializer {
            SyncModel(dao, remoteId)
        }
    }),
) {
    val remote = viewModel.uiState.collectAsState()

    SyncCompose(remote.value) { viewModel.viewModelScope.launch { viewModel.sync() } }

}

@Composable
private fun SyncCompose(
    remote: Remote,
    sync: () -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(all = 16.dp)
    ) {
        Text(remote.name)
        Text(remote.url)
        Text(remote.key)
        Text(remote.secret)
        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = {
                //context.startService(Intent(context, SyncService::class.java))
                sync
            }
        )
        {
            Text(text = "Sync")
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
        secret = "testtest"
    )


    ZimzyncTheme {
        SyncCompose(
            remote, sync = {}
        )
    }
}
