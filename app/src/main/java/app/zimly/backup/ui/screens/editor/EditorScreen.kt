package app.zimly.backup.ui.screens.editor

import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import app.zimly.backup.ui.screens.editor.form.field.BackupSourceField
import app.zimly.backup.ui.screens.editor.form.BucketForm
import app.zimly.backup.ui.theme.ZimzyncTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    remoteId: Int?,
    back: () -> Unit,
    viewModel: EditorViewModel = viewModel(
        factory = EditorViewModel.Factory,
        extras = MutableCreationExtras().apply {
            set(APPLICATION_KEY, LocalContext.current.applicationContext as Application)
            set(EditorViewModel.REMOTE_ID_KEY, remoteId)
        })
) {

    val state = viewModel.state.collectAsState()
    // want to go nuts?
    // https://afigaliyev.medium.com/snackbar-state-management-best-practices-for-jetpack-compose-1a5963d86d98
    val snackbarState = remember { SnackbarHostState() }

    EditorCompose(
        state,
        snackbarState,
        viewModel.bucketForm,
        backupSource = viewModel.backupSource,
        clearSnackbar = viewModel::clearSnackbar,
        save = {
            viewModel.viewModelScope.launch {
                viewModel.save(back)
            }
        },
        back,
        verify = { viewModel.viewModelScope.launch { viewModel.verify() } }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditorCompose(
    state: State<EditorViewModel.UiState>,
    snackbarState: SnackbarHostState,
    bucketForm: BucketForm,
    backupSource: BackupSourceField,
    clearSnackbar: () -> Unit,
    save: () -> Unit,
    back: () -> Unit,
    verify: () -> Unit,
) {
    // If the UI state contains an error, show snackbar
    if (state.value.notification != null) {
        LaunchedEffect(snackbarState) {
            val result = snackbarState.showSnackbar(
                message = state.value.notification!!,
                withDismissAction = true,
                duration = if (state.value.notificationError) SnackbarDuration.Indefinite else SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.Dismissed -> clearSnackbar()
                SnackbarResult.ActionPerformed -> clearSnackbar()
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
            BucketConfiguration(bucketForm, verify)
            BackupSourceConfiguration(backupSource)
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

        val bucketForm = BucketForm()
        val backupSource = BackupSourceField()

        EditorCompose(
            internal.collectAsState(),
            snackbarState,
            bucketForm,
            backupSource,
            clearSnackbar = {},
            save = {},
            back = {},
            verify = {},
        )
    }
}
