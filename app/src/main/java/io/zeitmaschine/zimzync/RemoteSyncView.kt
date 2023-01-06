package io.zeitmaschine.zimzync

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import kotlinx.coroutines.launch
import java.util.*

class SyncModel(private val repository: MinioRepository, val remote: Remote) : ViewModel() {

    companion object {
        val TAG: String? = SyncModel::class.simpleName
    }

    fun sync() {
        // Create a new coroutine on the UI thread
        viewModelScope.launch {

            // Display result of the minio request to the user
            when (val result = repository.listBuckets()) {
                is Result.Success<List<String>> -> Log.d(TAG, result.data.first())// Happy path
                else -> Log.e(TAG, "FML")// Show error in UI
            }
        }
    }
}


@Composable
fun SyncRemote(
    remote: Remote,
    viewModel: SyncModel = viewModel(factory = viewModelFactory {
        initializer {
            SyncModel(MinioRepository(remote.url, remote.key, remote.secret, "test-bucket"), remote)
        }
    }),
) {

    SyncCompose(viewModel = viewModel, context = LocalContext.current)
}

@Composable
private fun SyncCompose(
    viewModel: SyncModel,
    context: Context
) {

    val remote = viewModel.remote
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
                context.startService(Intent(context, SyncService::class.java))
                viewModel.sync()
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
    val bucket = "test-bucket"
    val viewModel = viewModel(initializer = {
        val remote = remote {
            id = UUID.randomUUID().toString()
            name = "zeitmaschine.io"
            url = "http://10.0.2.2:9000"
            key = "test"
            secret = "testtest"
            created = System.currentTimeMillis()
            modified = System.currentTimeMillis()
        }
        SyncModel(MinioRepository(remote.url, remote.key, remote.secret, bucket), remote)
    })

    ZimzyncTheme {
        SyncCompose(viewModel, context = LocalContext.current)
    }
}
