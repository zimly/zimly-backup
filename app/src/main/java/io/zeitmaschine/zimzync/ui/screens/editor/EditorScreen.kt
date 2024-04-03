package io.zeitmaschine.zimzync.ui.screens.editor

import android.annotation.SuppressLint
import android.app.Application
import android.webkit.URLUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.RemoteDao
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
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
