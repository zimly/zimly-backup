package io.zeitmaschine.zimzync.ui.screens.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.data.remote.RemoteDao
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import io.zeitmaschine.zimzync.ui.theme.containerBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

class MainViewModel(private val dataStore: RemoteDao) : ViewModel() {


    private val selected = mutableListOf<Int>()
    private val _selectedState = MutableStateFlow(emptyList<Int>())

    private val _remotes = flow {
        val items = dataStore.getAll()
        emit(items)
    }

    val remotesState = combine(_remotes, _selectedState)
    { remotes, sel -> remotes.map { RemoteView(it.uid!!, it.name, it.url, sel.contains(it.uid)) } }


    fun select(remoteId: Int) {
        if (selected.contains(remoteId)) {
            selected.remove(remoteId)
        } else {
            selected.add(remoteId)
        }
        // Trigger state update
        _selectedState.value = selected.toList()
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
    syncRemote: (Int) -> Unit,
    addRemote: () -> Unit,
) {
    val remotes = viewModel.remotesState.collectAsState(initial = emptyList())
    RemoteComponent(
        remotes = remotes.value,
        syncRemote = syncRemote,
        addRemote = addRemote
    ) { viewModel.select(it) }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RemoteComponent(
    remotes: List<RemoteView>,
    syncRemote: (Int) -> Unit,
    addRemote: () -> Unit,
    select: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Zimly",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                })
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                addRemote()
            }) {
                Icon(Icons.Filled.Add, "Add Remote")
            }
        })
    { innerPadding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier.fillMaxWidth()
        ) {
            items(remotes) { remote ->
                Box(
                    modifier = Modifier
                        // Note: Order matters!
                        .clip(RoundedCornerShape(12.dp))
                        .background(color = containerBackground())
                        .then(
                            if (remote.selected) Modifier.border(
                                width = Dp(2f),
                                color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(12.dp)
                            ) else Modifier
                        )
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { syncRemote(remote.uid) },
                            onLongClick = { select(remote.uid) })
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

}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ZimzyncTheme {
        val remotes = generateSequence(0) { it + 1 }.take(10).map {
            RemoteView(
                uid = it,
                name = "test $it",
                url = "https://blob.rawbot.zone/$it",
            )
        }.toList()

        RemoteComponent(remotes = remotes, syncRemote = {}, addRemote = {}) {}
    }
}

data class RemoteView(
    val uid: Int,
    val name: String,
    val url: String,
    val selected: Boolean = false
)