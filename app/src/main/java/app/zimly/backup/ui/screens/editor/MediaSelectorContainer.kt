package app.zimly.backup.ui.screens.editor

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.media.LocalMediaRepository
import app.zimly.backup.permission.PermissionService
import app.zimly.backup.ui.screens.editor.field.TextField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun MediaSelectorContainer(
    mediaField: TextField,
    viewModel: MediaSelectorViewModel = viewModel(
        factory = MediaSelectorViewModel.Factory
    )
) {
    val state by viewModel.state.collectAsState()
    val selectedCollection by mediaField.state.collectAsState()

    val select: (collection: String) -> Unit = {
        mediaField.update(it)
    }
    val focus: (state: FocusState) -> Unit = {
        mediaField.focus(it)
    }

    MediaSelector(selectedCollection.value, state, focus, select)
    if (!state.granted) {
        PermissionBox({ viewModel.onGranted(it) }, viewModel.getPermission())
    }
}

class MediaSelectorViewModel(
    private val mediaRepository: LocalMediaRepository,
    private val permissionService: PermissionService
) : ViewModel() {

    private val internal: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = internal.asStateFlow()

    init {
        val collections = mediaRepository.getBuckets()
        val granted = permissionService.isPermissionGranted()
        internal.update { it.copy(collections = collections.keys, granted = granted) }
    }

    fun onGranted(grants: Map<String, Boolean>) {
        val allGranted = permissionService.checkUserGrants(grants)
        val collections = mediaRepository.getBuckets()
        internal.update { it.copy(collections = collections.keys, granted = allGranted == true) }
    }

    fun getPermission(): Array<String> {
        return permissionService.getPermissions()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])

                val mediaRepository = LocalMediaRepository(application.contentResolver)
                val permissionService = PermissionService(application.applicationContext)
                MediaSelectorViewModel(mediaRepository, permissionService)
            }
        }
    }

    data class UiState(val collections: Set<String> = emptySet(), val granted: Boolean = false)
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MediaSelector(
    selectedCollection: String,
    state: MediaSelectorViewModel.UiState,
    onFocus: (FocusState) -> Unit,
    onSelect: (String) -> Unit,
) {

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            // The `menuAnchor` modifier must be passed to the text field for correctness.
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .onFocusChanged { onFocus(it) },
            readOnly = true,
            value = selectedCollection,
            onValueChange = {},
            label = { Text("Collection") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.collections.forEach { gallery ->
                DropdownMenuItem(
                    text = { Text(gallery) },
                    onClick = {
                        onSelect(gallery)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun PermissionBox(
    onGranted: (grants: Map<String, Boolean>) -> Unit,
    permissions: Array<String>
) {

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        onGranted(grants)
    }

    Column {
        Text("Access to media collections not granted.")
        TextButton(
            onClick = {
                permissionLauncher.launch(permissions)
            },
            contentPadding = PaddingValues(
                horizontal = 16.dp,
            ),
        ) {
            Text(text = "Grant Permission")
        }
    }
}
