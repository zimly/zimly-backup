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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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

class EditorModel(private val dataStore: DataStore<Remotes>, remoteId: String?) : ViewModel() {

    var remote: Flow<Remote?> = flowOf(Remote.getDefaultInstance())

    init {
        viewModelScope.launch {
            remoteId?.let {
                remote = get { remote -> remote.name.equals(remoteId) }
            }
        }
    }
    fun get(where: (Remote) -> Boolean): Flow<Remote?> {
        return dataStore.data
            .map { remotes -> remotes.remotesList }
            .map { r -> r.first(where) }
    }

    suspend fun saveEntry(remote: Remote) {
        dataStore.updateData { currentRemotes ->
            currentRemotes.toBuilder()
                .addRemotes(remote)
                .build()
        }
    }
}

@Composable
fun EditRemote(dataStore: DataStore<Remotes>,
               viewModel: EditorModel = viewModel(factory = viewModelFactory {
                   initializer {
                       EditorModel(dataStore, remoteId)
                   }
               }),
               remoteId: String?, saveEntry: () -> Unit) {

    val remote = viewModel.remote.collectAsState(initial = remote {})

    remote.value?.let { EditorCompose(it, saveEntry) }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditorCompose(
    remote: Remote,
    saveEntry: () -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(all = 16.dp)
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            value = remote.name,
            onValueChange = {},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL") },
            value = remote.url,
            onValueChange = {},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Key") },
            value = remote.key,
            onValueChange = {},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Secret") },
            value = remote.secret,
            onValueChange = {},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = saveEntry
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
        EditorCompose(remote = remote {}, saveEntry = {})
    }
}
