package io.zeitmaschine.zimzync

import android.annotation.SuppressLint
import android.app.Application
import android.webkit.URLUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
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

class EditorModel(application: Application, private val dao: RemoteDao, remoteId: Int?) :
    AndroidViewModel(application) {

    private val contentResolver by lazy { application.contentResolver }
    private val mediaRepo: MediaRepository = ResolverBasedRepository(contentResolver)

    // https://stackoverflow.com/questions/69689843/jetpack-compose-state-hoisting-previews-and-viewmodels-best-practices
    // TODO ???? https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate
    // Internal mutable state
    private val internal: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    // Expose Ui State
    val state: StateFlow<UiState> = internal.asStateFlow()

    init {
        val galleries = mediaRepo.getBuckets().keys
        internal.update {
            it.copy(galleries = galleries)
        }

        remoteId?.let {
            viewModelScope.launch {
                val remote = dao.loadById(remoteId)

                internal.update {
                    it.copy(
                        uid = remote.uid,
                        name = remote.name,
                        url = remote.url,
                        key = remote.key,
                        secret = remote.secret,
                        bucket = remote.bucket,
                        folder = remote.folder,
                    )
                }
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

    fun setFolder(folder: String) {
        internal.update { it.copy(folder = folder) }
    }


    suspend fun save() {
        val remote = Remote(
            internal.value.uid,
            internal.value.name,
            internal.value.url,
            internal.value.key,
            internal.value.secret,
            internal.value.bucket,
            internal.value.folder
        )
        if (remote.uid == null) {
            dao.insert(remote)
        } else {
            dao.update(remote)
        }
    }

    data class UiState(
        var uid: Int? = null,
        var name: String = "",
        var url: String = "",
        var key: String = "",
        var secret: String = "",
        var bucket: String = "",
        var folder: String = "",
        var galleries: Set<String> = emptySet()
    )
}

@Composable
fun EditRemote(
    application: Application,
    remoteDao: RemoteDao,
    remoteId: Int?,
    viewModel: EditorModel = viewModel(factory = viewModelFactory {
        initializer {
            EditorModel(application, remoteDao, remoteId)
        }
    }),
    back: () -> Unit,
) {

    val state = viewModel.state.collectAsState()
    EditorCompose(
        state,
        setName = viewModel::setName,
        setUrl = viewModel::setUrl,
        setKey = viewModel::setKey,
        setSecret = viewModel::setSecret,
        setBucket = viewModel::setBucket,
        setFolder = viewModel::setFolder,
        save = {
            viewModel.viewModelScope.launch {
                viewModel.save()
                back()
            }
        },
        back
    ) }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditorCompose(
    state: State<EditorModel.UiState>,
    setName: (name: String) -> Unit,
    setUrl: (url: String) -> Unit,
    setKey: (key: String) -> Unit,
    setSecret: (secret: String) -> Unit,
    setBucket: (secret: String) -> Unit,
    setFolder: (folder: String) -> Unit,
    save: () -> Unit,
    back: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = if (state.value.uid != null) state.value.name else "New configuration",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { back() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            contentDescription = "Go Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { save() }) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            contentDescription = "Save or Create Remote"
                        )
                    }

                }
            )
        }) { innerPadding ->

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier.fillMaxWidth(),
        ) {

            BucketConfiguration(state, setName, setUrl, setKey, setSecret, setBucket)
            FolderConfiguration(state, setFolder)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FolderConfiguration(
    state: State<EditorModel.UiState>,
    setFolder: (folder: String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Outlined.Photo,
                "Media",
                modifier = Modifier.padding(top = 8.dp, end = 8.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    // The `menuAnchor` modifier must be passed to the text field for correctness.
                    modifier = Modifier.menuAnchor() then Modifier.fillMaxWidth(),
                    readOnly = true,
                    value = state.value.folder,
                    onValueChange = {},
                    label = { Text("Folder") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    state.value.galleries.forEach { gallery ->
                        DropdownMenuItem(
                            text = { Text(gallery) },
                            onClick = {
                                setFolder(gallery)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BucketConfiguration(
    state: State<EditorModel.UiState>,
    setName: (name: String) -> Unit,
    setUrl: (url: String) -> Unit,
    setKey: (key: String) -> Unit,
    setSecret: (secret: String) -> Unit,
    setBucket: (secret: String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Outlined.CloudUpload,
                "Media",
                modifier = Modifier.padding(top = 8.dp, end = 8.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                value = state.value.name,
                onValueChange = { setName(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = state.value.name.isBlank()
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL") },
                value = state.value.url,
                onValueChange = { setUrl(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = !URLUtil.isValidUrl(state.value.url)
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Key") },
                value = state.value.key,
                onValueChange = { setKey(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = state.value.key.isBlank()
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Secret") },
                value = state.value.secret,
                onValueChange = { setSecret(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                isError = state.value.secret.isBlank()
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bucket") },
                value = state.value.bucket,
                onValueChange = { setBucket(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = state.value.bucket.isBlank()
            )
        }
    }
}


@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun EditPreview() {
    ZimzyncTheme {
        val internal: MutableStateFlow<EditorModel.UiState> =
            MutableStateFlow(EditorModel.UiState())

        EditorCompose(
            internal.collectAsState(),
            setName = { name -> internal.update { it.copy(name = name) } },
            setUrl = { url -> internal.update { it.copy(url = url) } },
            setKey = { key -> internal.update { it.copy(key = key) } },
            setSecret = { secret -> internal.update { it.copy(secret = secret) } },
            setBucket = { bucket -> internal.update { it.copy(bucket = bucket) } },
            setFolder = { folder -> internal.update { it.copy(folder = folder) } },
            save = {},
            back = {},
        )
    }
}
