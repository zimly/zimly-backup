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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
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

    val name: Field = Field()
    val url: Field = Field(
        errorMessage = "Not a valid URL.",
        validate = { URLUtil.isValidUrl(it) })
    val key: Field = Field()
    val secret: Field = Field()
    val bucket: Field = Field()
    val folder: Field = Field(errorMessage = "Select a media gallery to synchronize.")


    init {
        val galleries = mediaRepo.getBuckets().keys
        internal.update {
            it.copy(
                galleries = galleries,
                title = "New configuration"
            )
        }

        remoteId?.let {
            viewModelScope.launch {
                val remote = dao.loadById(remoteId)

                internal.update {
                    it.copy(
                        uid = remote.uid,
                        title = remote.name,
                    )
                }
                name.update(remote.name)
                url.update(remote.url)
                key.update(remote.key)
                secret.update(remote.secret)
                bucket.update(remote.bucket)
                folder.update(remote.folder)

            }
        }
    }

    suspend fun save(success: () -> Unit) {
        val valid =
            name.isValid() && url.isValid() && key.isValid() && secret.isValid() && bucket.isValid() && folder.isValid()
        if (valid) {
            val remote = Remote(
                internal.value.uid,
                name.state.value.value, // TODO from state vs internal state?
                url.state.value.value,
                key.state.value.value,
                secret.state.value.value,
                bucket.state.value.value,
                folder.state.value.value,
            )
            if (remote.uid == null) {
                dao.insert(remote)
            } else {
                dao.update(remote)
            }
            success()
        } else {
            internal.update { it.copy(error = "Form has errors, won't save.") }
        }
    }

    fun clearError() {
        internal.update { it.copy(error = "") }
    }


    data class UiState(
        var uid: Int? = null,
        var title: String = "",
        var galleries: Set<String> = emptySet(),
        var error: String = "",
    )

    class Field(
        private val errorMessage: String = "This field is required.",
        private val validate: (value: String) -> Boolean = { it.isNotEmpty() },
    ) {
        private var touched: Boolean? = null
        private val internal: MutableStateFlow<FieldState> = MutableStateFlow(FieldState())
        val state: StateFlow<FieldState> = internal.asStateFlow()

        fun update(value: String) {
            internal.update {
                it.copy(
                    value = value,
                    error = if (isError()) errorMessage else null
                )
            }
        }

        fun focus(focus: FocusState) {
            if (touched == null && focus.hasFocus) {
                touched = false
            } else if (touched == false && !focus.hasFocus) {
                touched = true
            }
            if (isError()) {
                internal.update {
                    it.copy(
                        error = errorMessage
                    )
                }
            }
        }

        private fun isError(): Boolean {
            return touched == true && !isValid()
        }

        fun isValid(): Boolean {
            return validate(internal.value.value)
        }


        data class FieldState(val value: String = "", val error: String? = null)
    }
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
    // want to go nuts?
    // https://afigaliyev.medium.com/snackbar-state-management-best-practices-for-jetpack-compose-1a5963d86d98
    val snackbarState = remember { SnackbarHostState() }

    EditorCompose(
        state,
        snackbarState,
        name = viewModel.name,
        url = viewModel.url,
        key = viewModel.key,
        secret = viewModel.secret,
        bucket = viewModel.bucket,
        folder = viewModel.folder,
        clearError = viewModel::clearError,
        save = {
            viewModel.viewModelScope.launch {
                viewModel.save(back)
            }
        },
        back
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditorCompose(
    state: State<EditorModel.UiState>,
    snackbarState: SnackbarHostState,
    name: EditorModel.Field,
    url: EditorModel.Field,
    key: EditorModel.Field,
    secret: EditorModel.Field,
    bucket: EditorModel.Field,
    folder: EditorModel.Field,
    clearError: () -> Unit,
    save: () -> Unit,
    back: () -> Unit,
) {
    // If the UI state contains an error, show snackbar
    if (state.value.error.isNotEmpty()) {
        LaunchedEffect(snackbarState) {
            val result = snackbarState.showSnackbar(
                message = state.value.error,
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite
            )
            when (result) {
                SnackbarResult.Dismissed -> clearError()
                SnackbarResult.ActionPerformed -> clearError()
            }

        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.value.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { back() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { save() }) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save or Create Remote"
                        )
                    }

                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }
    ) { innerPadding ->

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier.fillMaxWidth(),
        ) {

            BucketConfiguration(name, url, key, secret, bucket)
            FolderConfiguration(state, folder)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FolderConfiguration(
    state: State<EditorModel.UiState>,
    folder: EditorModel.Field
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
            val folderState = folder.state.collectAsState()

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    // The `menuAnchor` modifier must be passed to the text field for correctness.
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .onFocusChanged { folder.focus(it) },
                    readOnly = true,
                    value = folderState.value.value,
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
                                folder.update(gallery)
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
    name: EditorModel.Field,
    url: EditorModel.Field,
    key: EditorModel.Field,
    secret: EditorModel.Field,
    bucket: EditorModel.Field
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
            val nameState = name.state.collectAsState()
            val urlState = url.state.collectAsState()
            val keyState = key.state.collectAsState()
            val secretState = secret.state.collectAsState()
            val bucketState = bucket.state.collectAsState()

            OutlinedTextField(
                modifier = Modifier
                    .onFocusChanged { name.focus(it) }
                    .fillMaxWidth(),
                label = { Text("Name") },
                value = nameState.value.value,
                onValueChange = { name.update(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = nameState.value.error != null,
                supportingText = { nameState.value.error?.let { Text(it) } }
            )
            OutlinedTextField(
                modifier = Modifier
                    .onFocusChanged { url.focus(it) }
                    .fillMaxWidth(),
                label = { Text("URL") },
                value = urlState.value.value,
                onValueChange = { url.update(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = urlState.value.error != null,
                supportingText = { urlState.value.error?.let { Text(it) } }
            )
            OutlinedTextField(
                modifier = Modifier
                    .onFocusChanged { key.focus(it) }
                    .fillMaxWidth(),
                label = { Text("Key") },
                value = keyState.value.value,
                onValueChange = { key.update(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = keyState.value.error != null,
                supportingText = { keyState.value.error?.let { Text(it) } }
            )
            OutlinedTextField(
                modifier = Modifier
                    .onFocusChanged { secret.focus(it) }
                    .fillMaxWidth(),
                label = { Text("Secret") },
                value = secretState.value.value,
                onValueChange = { secret.update(it) },
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
                isError = secretState.value.error != null,
                supportingText = { secretState.value.error?.let { Text(it) } }
            )
            OutlinedTextField(
                modifier = Modifier
                    .onFocusChanged { bucket.focus(it) }
                    .fillMaxWidth(),
                label = { Text("Bucket") },
                value = bucketState.value.value,
                onValueChange = { bucket.update(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = bucketState.value.error != null,
                supportingText = { bucketState.value.error?.let { Text(it) } }
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

        val snackbarState = remember { SnackbarHostState() }

        val name: EditorModel.Field = EditorModel.Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val url: EditorModel.Field = EditorModel.Field(
            errorMessage = "Not a valid URL.",
            validate = { URLUtil.isValidUrl(it) })
        val key: EditorModel.Field = EditorModel.Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val secret: EditorModel.Field = EditorModel.Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val bucket: EditorModel.Field = EditorModel.Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val folder: EditorModel.Field = EditorModel.Field(
            errorMessage = "Select a media gallery to synchronize.",
            validate = { it.isNotEmpty() })

        EditorCompose(
            internal.collectAsState(),
            snackbarState,
            name,
            url,
            key,
            secret,
            bucket,
            folder,
            clearError = {},
            save = {},
            back = {},
        )
    }
}
