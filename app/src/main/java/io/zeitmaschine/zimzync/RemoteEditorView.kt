package io.zeitmaschine.zimzync

import android.annotation.SuppressLint
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorModel(private val dao: RemoteDao, remoteId: Int?) : ViewModel() {

    // https://stackoverflow.com/questions/69689843/jetpack-compose-state-hoisting-previews-and-viewmodels-best-practices
    // TODO ???? https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate
    // Internal mutable state
    private val internal: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    // Expose Ui State
    val state: StateFlow<UiState> = internal.asStateFlow()

    init {
        remoteId?.let {
            viewModelScope.launch {
                val remote = dao.loadById(remoteId)
                internal.value.uid = remote.uid
                internal.value.name = remote.name
                internal.value.url = remote.url
                internal.value.key = remote.key
                internal.value.secret = remote.secret
            }
        }
    }

    fun setName(name: String) {
        internal.update { it.copy(name = name) }
    }

    fun setUrl(url: String) {
        internal.update { it.copy(url = url) }
    }

    fun setKey(key: String) {
        internal.update { it.copy(key = key) }
    }

    fun setSecret(secret: String) {
        internal.update { it.copy(secret = secret) }
    }

    fun setBucket(bucket: String) {
        internal.update { it.copy(bucket = bucket) }
    }


    suspend fun save() {
        val remote = Remote(
            internal.value.uid,
            internal.value.name,
            internal.value.url,
            internal.value.key,
            internal.value.secret,
            internal.value.bucket
        )
        if (remote.uid == null) {
            dao.insert(remote)
        } else {
            dao.update(remote)
        }
    }
}

data class UiState(
    var uid: Int? = null,
    var name: String = "",
    var url: String = "",
    var key: String = "",
    var secret: String = "",
    var bucket: String = ""
)

@Composable
fun EditRemote(
    remoteDao: RemoteDao,
    remoteId: Int?,
    viewModel: EditorModel = viewModel(factory = viewModelFactory {
        initializer {
            EditorModel(remoteDao, remoteId)
        }
    }),
    saveEntry: () -> Unit
) {

    val state = viewModel.state.collectAsState()
    EditorCompose(
        state,
        setName = viewModel::setName,
        setUrl = viewModel::setUrl,
        setKey = viewModel::setKey,
        setSecret = viewModel::setSecret,
        setBucket = viewModel::setBucket
    ) {
        viewModel.viewModelScope.launch {
            viewModel.save()
            saveEntry()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditorCompose(
    state: State<UiState>,
    setName: (name: String) -> Unit,
    setUrl: (url: String) -> Unit,
    setKey: (key: String) -> Unit,
    setSecret: (secret: String) -> Unit,
    setBucket: (secret: String) -> Unit,
    save: () -> Unit,
) {


    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(all = 16.dp)
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            value = state.value.name,
            onValueChange = { setName(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL") },
            value = state.value.url,
            onValueChange = { setUrl(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Key") },
            value = state.value.key,
            onValueChange = { setKey(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Secret") },
            value = state.value.secret,
            onValueChange = { setSecret(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Bucket") },
            value = state.value.bucket,
            onValueChange = { setBucket(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )

        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = {
                save()
            }
        )
        {
            Text(text = "Save")
        }
    }
}


@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun EditPreview() {
    ZimzyncTheme {
        val internal: MutableStateFlow<UiState> = MutableStateFlow(UiState())

        EditorCompose(
            internal.collectAsState(),
            setName = { name -> internal.update { it.copy(name = name) } },
            setUrl = { url -> internal.update { it.copy(url = url) } },
            setKey = { key -> internal.update { it.copy(key = key) } },
            setSecret = { secret -> internal.update { it.copy(secret = secret) } },
            setBucket = { bucket -> internal.update { it.copy(bucket = bucket) } },
        ) {}
    }
}
