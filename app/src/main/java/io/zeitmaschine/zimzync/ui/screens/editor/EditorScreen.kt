package io.zeitmaschine.zimzync.ui.screens.editor

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.RemoteDao
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import io.zeitmaschine.zimzync.ui.theme.containerBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    application: Application,
    remoteDao: RemoteDao,
    remoteId: Int?,
    viewModel: EditorViewModel = viewModel(factory = viewModelFactory {
        initializer {
            EditorViewModel(application, remoteDao, remoteId)
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
    state: State<EditorViewModel.UiState>,
    snackbarState: SnackbarHostState,
    name: EditorViewModel.Field,
    url: EditorViewModel.Field,
    key: EditorViewModel.Field,
    secret: EditorViewModel.Field,
    bucket: EditorViewModel.Field,
    folder: EditorViewModel.Field,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
    state: State<EditorViewModel.UiState>,
    folder: EditorViewModel.Field
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
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
    name: EditorViewModel.Field,
    url: EditorViewModel.Field,
    key: EditorViewModel.Field,
    secret: EditorViewModel.Field,
    bucket: EditorViewModel.Field
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
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
        val internal: MutableStateFlow<EditorViewModel.UiState> =
            MutableStateFlow(EditorViewModel.UiState())

        val snackbarState = remember { SnackbarHostState() }

        val name: EditorViewModel.Field = EditorViewModel.Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val url: EditorViewModel.Field = EditorViewModel.Field(
            errorMessage = "Not a valid URL.",
            validate = { URLUtil.isValidUrl(it) })
        val key: EditorViewModel.Field = EditorViewModel.Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val secret: EditorViewModel.Field = EditorViewModel.Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val bucket: EditorViewModel.Field = EditorViewModel.Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val folder: EditorViewModel.Field = EditorViewModel.Field(
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
