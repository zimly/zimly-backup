package io.zeitmaschine.zimzync

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
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.minio.BucketExistsArgs
import io.minio.MinioClient
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*

class SyncModel(private val dataStore: DataStore<Remotes>, remoteId: String?) : ViewModel() {

    lateinit var remote: Flow<Remote?>

    init {
        viewModelScope.launch {
            remoteId?.let {
                remote = get { remote -> remote.id.equals(remoteId) }
            }
        }
    }

    fun get(where: (Remote) -> Boolean): Flow<Remote?> {
        return dataStore.data
            .map { remotes -> remotes.remotesList }
            .map { r -> r.first(where) }
    }

}

fun sync(url: String, key: String, secret: String, bucket: String) {
    try {
        // Create a minioClient with the MinIO server playground, its access key and secret key.
        val mc: MinioClient = MinioClient.builder()
            .endpoint(url)
            .credentials(key, secret)
            .build()

        val found = mc.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            System.out.println("Bucket doesn't exists.");
        } else {
            System.out.println("Bucket already exists.");
        }
        mc.listBuckets().forEach { bucket -> System.out.println(bucket.name()) }
    } catch (e: Exception) {
        System.out.println(e.message)
    }

}

@Composable
fun SyncRemote(
    dataStore: DataStore<Remotes>,
    viewModel: SyncModel = viewModel(factory = viewModelFactory {
        initializer {
            SyncModel(dataStore, remoteId)
        }
    }), remoteId: String?
) {

    val remote: State<Remote?> = viewModel.remote.collectAsState(initial = remote {})

    remote.value?.let {
        SyncCompose(remote = it) {
            viewModel.viewModelScope.launch {
                {} // TODO
            }
        }
    }
}

@Composable
private fun SyncCompose(
    remote: Remote,
    sync: () -> Unit // TODO
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
                sync(remote.url, remote.key, remote.secret, "test-bucket")
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
    val s = "zeitmaschine.io"
    val s1 = "http://10.0.2.2:9000"
    val s2 = "test"
    val s3 = "testtest"
    val bucket = "test-bucket"
    ZimzyncTheme {
        SyncCompose(remote = remote {
                id = UUID.randomUUID().toString()
                name = s
                url = s1
                key = s2
                secret = s3
                created = System.currentTimeMillis()
                modified = System.currentTimeMillis()
        }) {
            sync(s1, s2, s3, bucket)
        }
    }
}