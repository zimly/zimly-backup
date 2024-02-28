package io.zeitmaschine.zimzync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import io.zeitmaschine.zimzync.ui.theme.containerBackground
import kotlinx.coroutines.flow.flow

class MainViewModel(private val dataStore: RemoteDao) : ViewModel() {

    val uiState = flow {
        val items = dataStore.getAll()
        emit(items)
    }
}

@Composable
fun RemoteScreen(
    remoteDao: RemoteDao,
    // https://programmer.ink/think/a-new-way-to-create-a-viewmodel-creationextras.html
    viewModel: MainViewModel = viewModel(factory = viewModelFactory {
        initializer {
            MainViewModel(remoteDao)
        }
    }),
    openSync: (Int) -> Unit,
) {
    val remotes = viewModel.uiState.collectAsState(initial = emptyList())
    RemoteComponent(remotes = remotes.value, openSync = openSync)
}

@Composable
fun RemoteComponent(remotes: List<Remote>, openSync: (Int) -> Unit,) {

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(remotes) { remote ->
            Box(
                modifier = Modifier
                    // Note: Order matters!
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = containerBackground())
                    .fillMaxWidth()
                    .clickable(onClick = { if (remote.uid != null) openSync(remote.uid) })
                    .padding(16.dp)
                ) {
                Column {
                    Text(remote.name, color = MaterialTheme.colorScheme.onSurface)
                    Text(remote.url, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ZimzyncTheme {
        val remotes = generateSequence(0) { it + 1 }.take(10).map {
            Remote(
                uid = it,
                name = "test $it",
                url = "https://blob.rawbot.zone/$it",
                key = "key",
                secret = "secret",
                bucket = "bucket-name",
                folder = "Pictures"
            )
        }.toList()

        RemoteComponent(remotes = remotes, openSync = {})
    }
}