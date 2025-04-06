package app.zimly.backup.ui.screens.editor

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    val context = LocalContext.current
    val permissionsDenied = viewModel.isAnyPermissionPermanentlyDenied(context)

    MediaSelector(selectedCollection.value, state, focus, select)
    if (!state.granted) {
        PermissionBox(
            permissionsDenied,
            viewModel.getPermission(),
            { viewModel.onGranted(it) },
            { viewModel.openSettings(context) },
        )
    }
}

class MediaSelectorViewModel(
    private val mediaRepository: LocalMediaRepository,
    private val permissionService: PermissionService,
    private val packageName: String
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

    fun isAnyPermissionPermanentlyDenied(context: Context): Boolean {
        val activity =
            context as? Activity ?: (context as? ContextWrapper)?.baseContext as? Activity

        return activity?.let {
            permissionService.getPermissions().any { perm ->
                ContextCompat.checkSelfPermission(it, perm) != PackageManager.PERMISSION_GRANTED &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(it, perm)
            }
        } ?: false
    }


    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])

                val mediaRepository = LocalMediaRepository(application.contentResolver)
                val permissionService = PermissionService(application.applicationContext)
                val packageName = application.packageName
                MediaSelectorViewModel(mediaRepository, permissionService, packageName)
            }
        }
    }

    data class UiState(
        val collections: Set<String> = emptySet(),
        val granted: Boolean = false,
        val permissionsDenied: Boolean = false
    )
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
    permissionsPermanentlyDenied: Boolean,
    permissions: Array<String>,
    onGranted: (grants: Map<String, Boolean>) -> Unit,
    openSettings: () -> Unit
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
                if (permissionsPermanentlyDenied) openSettings() else permissionLauncher.launch(
                    permissions
                )
            },
            contentPadding = PaddingValues(
                horizontal = 16.dp,
            ),
        ) {
            Text(text = if (permissionsPermanentlyDenied) "Open Settings" else "Grant Permission")
        }
    }
}
