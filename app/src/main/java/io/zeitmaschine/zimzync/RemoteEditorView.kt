package io.zeitmaschine.zimzync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class EditorModel(private val dataStore: DataStore<Remotes>, remoteId: String?) : ViewModel() {

    var remote: Flow<Remote?> = flowOf(Remote.getDefaultInstance())

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

    // FIXME? https://www.rrtutors.com/tutorials/implement-room-database-in-jetpack-compose
    suspend fun saveEntry(remote: Remote) {
        dataStore.updateData { currentRemotes ->
            var i = currentRemotes.remotesList.indexOfFirst { r -> r.id.equals(remote.id) }
            if (i > -1) {
                currentRemotes.toBuilder()
                    .removeRemotes(i)
                    .addRemotes(remote)
                    .build()
            } else {
                currentRemotes.toBuilder()
                    .addRemotes(remote)
                    .build()
            }

        }
    }
}

@Composable
fun EditRemote(
    dataStore: DataStore<Remotes>,
    viewModel: EditorModel = viewModel(factory = viewModelFactory {
        initializer {
            EditorModel(dataStore, remoteId)
        }
    }),
    remoteId: String?, saveEntry: (remote: Remote) -> Unit
) {


    val remote: State<Remote?> = viewModel.remote.collectAsState(initial = remote {})


    remote.value?.let {
        EditorCompose(remote = it) { remote ->
            viewModel.viewModelScope.launch {
                viewModel.saveEntry(remote)
            }
            saveEntry(remote)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditorCompose(
    remote: Remote,
    saveEntry: (remote: Remote) -> Unit
) {

    var eName by remember { mutableStateOf(remote.name) }
    var eUrl by remember { mutableStateOf(remote.url) }
    var eKey by remember { mutableStateOf(remote.key) }
    var eSecret by remember { mutableStateOf(remote.secret) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(all = 16.dp)
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            value = eName,
            onValueChange = { eName = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL") },
            value = eUrl,
            onValueChange = { eUrl = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Key") },
            value = eKey,
            onValueChange = { eKey = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Secret") },
            value = eSecret,
            onValueChange = { eSecret = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = {

                var now = System.currentTimeMillis()
                var eCreated: Long = if (remote.created == 0L) {
                    now
                } else {
                    remote.created
                }
                var eId = remote.id
                // new entry
                if (remote.id.equals("")) {
                    eId = UUID.randomUUID().toString()
                }
                saveEntry( remote {
                    id = eId
                    name = eName
                    url = eUrl
                    key = eKey
                    secret = eSecret
                    created = eCreated
                    modified = now
                })
            }
        )
        {
            Text(text = "Save")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun EditPreview() {
    ZimzyncTheme {
        EditorCompose(remote = remote {}) {}
    }
}
