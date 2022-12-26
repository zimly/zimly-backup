package io.zeitmaschine.zimzync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import kotlinx.coroutines.flow.map

class MainViewModel(private val dataStore: DataStore<Remotes>) : ViewModel() {
    val remotes = dataStore.data.map { remotes -> remotes.remotesList }
}

@Composable
fun RemoteScreen(
    dataStore: DataStore<Remotes>,
    // https://programmer.ink/think/a-new-way-to-create-a-viewmodel-creationextras.html
    viewModel: MainViewModel = viewModel(factory = viewModelFactory {
        initializer {
            MainViewModel(dataStore)
        }
    }),
    editEntry: (String) -> Unit
) {
    val remotes = viewModel.remotes.collectAsState(initial = emptyList())
    RemoteComponent(remotes = remotes.value, editEntry)
}

@Composable
fun RemoteComponent(remotes: List<Remote>, editEntry: (String) -> Unit) {

    LazyColumn {
        items(remotes) { remote ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clickable(onClick = { editEntry(remote.id) })
            ) {
                Column {
                    Text(remote.name)
                    Text(remote.url)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ZimzyncTheme {
        val remotes = emptyList<Remote>()

        RemoteComponent(remotes = remotes, editEntry = {})
    }
}