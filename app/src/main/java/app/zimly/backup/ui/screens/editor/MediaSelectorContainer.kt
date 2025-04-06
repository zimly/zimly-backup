package app.zimly.backup.ui.screens.editor

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.media.LocalMediaRepository
import app.zimly.backup.permission.PermissionService
import app.zimly.backup.ui.components.PermissionBox
import app.zimly.backup.ui.components.PermissionRationaleDialog
import app.zimly.backup.ui.screens.editor.field.TextField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MediaSelectorViewModel(
    private val mediaRepository: LocalMediaRepository,
    private val permissionService: PermissionService,
    private val packageName: String
) : ViewModel() {

    private val internal: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = internal.asStateFlow()

    init {
        updateState()
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

    fun openDialog() {
        internal.update { it.copy(showDialog = true) }
    }

    fun closeDialog() {
        internal.update { it.copy(showDialog = false) }
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }

    fun updateState() {
        val collections = mediaRepository.getBuckets()
        val granted = permissionService.isPermissionGranted()
        internal.update { it.copy(collections = collections.keys, granted = granted) }
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
        val showDialog: Boolean = false,
    )
}

@Composable
fun MediaSelectorContainer(
    mediaField: TextField,
    viewModel: MediaSelectorViewModel = viewModel(
        factory = MediaSelectorViewModel.Factory
    )
) {
    val state by viewModel.state.collectAsState()
    val selectedCollection by mediaField.state.collectAsState()
    val context = LocalContext.current

    // Observe lifecycle changes to trigger the permission check when the activity resumes
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateState() // Check permission on resume
            }
        }

        // Adding observer
        lifecycleOwner.lifecycle.addObserver(observer)

        // Cleanup when the composable leaves the composition
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val select: (collection: String) -> Unit = {
        mediaField.update(it)
    }
    val focus: (state: FocusState) -> Unit = {
        mediaField.focus(it)
    }

    // TODO remember
    val permissionsDenied = viewModel.isAnyPermissionPermanentlyDenied(context)

    if (state.granted) {
        MediaSelector(selectedCollection.value, state, focus, select)
    } else {
        PermissionBox { viewModel.openDialog() }

        if (state.showDialog) {
            PermissionRationaleDialog(
                permissionsDenied,
                viewModel.getPermission(),
                { viewModel.closeDialog() },
                { viewModel.onGranted(it) },
                { viewModel.openSettings(context) }
            )
        }
    }
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

