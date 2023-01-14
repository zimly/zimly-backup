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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val dataStore: RemoteDao) : ViewModel() {

    var uiState: MutableStateFlow<List<Remote>> = MutableStateFlow(emptyList())
    init {
        viewModelScope.launch {
            var remotes = fetchAll()
            uiState.value = remotes
        }
    }
    private suspend fun fetchAll(): List<Remote> {
        return dataStore.getAll()
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
    openSync: (Int) -> Unit
) {
    val remotes = viewModel.uiState.collectAsState()
    RemoteComponent(remotes = remotes.value, openSync = openSync)
}

@Composable
fun RemoteComponent(remotes: List<Remote>, openSync: (Int) -> Unit) {

    LazyColumn {
        items(remotes) { remote ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clickable(onClick = { if (remote.uid != null) openSync(remote.uid) })
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

        RemoteComponent(remotes = remotes, openSync = {})
    }
}