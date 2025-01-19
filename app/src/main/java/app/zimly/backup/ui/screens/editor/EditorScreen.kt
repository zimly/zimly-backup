package app.zimly.backup.ui.screens.editor

import android.app.Application
import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.data.remote.RemoteDao
import app.zimly.backup.ui.screens.editor.field.BackupSourceField
import app.zimly.backup.ui.screens.editor.field.Field
import app.zimly.backup.ui.theme.ZimzyncTheme
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
        backupSource = viewModel.backupSource,
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
    name: Field,
    url: Field,
    key: Field,
    secret: Field,
    bucket: Field,
    backupSource: BackupSourceField,
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            ) then Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            BucketConfiguration(name, url, key, secret, bucket)

            val sourceSelector: @Composable (type: SourceType) -> Unit = { type ->
                when (type) {
                    SourceType.MEDIA -> {
                        val selectCollection: (collection: String) -> Unit = {
                            backupSource.update(SourceType.MEDIA)
                            backupSource.mediaField.update(it)
                        }
                        val focusCollection: (state: FocusState) -> Unit = {
                            backupSource.mediaField.focus(it)
                        }
                        val collection = backupSource.mediaField.state.collectAsState()
                        MediaCollectionSelector(
                            state.value.mediaCollections, collection.value.value,
                            selectCollection, focusCollection
                        )
                    }

                    SourceType.FOLDER -> {
                        val selectFolder: (folder: Uri) -> Unit = {
                            backupSource.update(SourceType.FOLDER)
                            backupSource.folderField.update(it)
                        }
                        val focusFolder: (state: FocusState) -> Unit = {
                            backupSource.folderField.focus(it)
                        }
                        val folder = backupSource.folderField.state.collectAsState()
                        DocumentsFolderSelector(folder.value.value, selectFolder, focusFolder)
                    }
                }
            }

            BackupSourceConfiguration(sourceSelector)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun EditPreview() {
    ZimzyncTheme {
        val internal: MutableStateFlow<EditorViewModel.UiState> =
            MutableStateFlow(EditorViewModel.UiState())

        val snackbarState = remember { SnackbarHostState() }

        val name = Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val url = Field(
            errorMessage = "Not a valid URL.",
            validate = { URLUtil.isValidUrl(it) })
        val key = Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val secret = Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val bucket = Field(
            errorMessage = "This field is required.",
            validate = { it.isNotEmpty() })
        val backupSource = BackupSourceField(
            errorMessage = "Select a media collection or folder to synchronize."
        )

        EditorCompose(
            internal.collectAsState(),
            snackbarState,
            name,
            url,
            key,
            secret,
            bucket,
            backupSource,
            clearError = {},
            save = {},
            back = {},
        )
    }
}
