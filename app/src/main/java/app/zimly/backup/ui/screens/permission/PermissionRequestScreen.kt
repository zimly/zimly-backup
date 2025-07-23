package app.zimly.backup.ui.screens.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.permission.MediaPermissionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionRequestViewModel(private val permissionService: MediaPermissionService) : ViewModel() {

    private val _requestPermissions = MutableStateFlow(!permissionService.permissionsGranted())

    val requestPermissions: StateFlow<Boolean> = _requestPermissions.asStateFlow()

    fun onGranted(grants: Map<String, Boolean>) {
        val allGranted = permissionService.verifyGrants(grants)
        _requestPermissions.value = allGranted
    }

    fun getPermissions(): Array<String> {
        return permissionService.requiredPermissions()
    }

    companion object {

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])

                val permissionService = MediaPermissionService(
                    application.applicationContext,
                    application.packageName
                )
                PermissionRequestViewModel(permissionService)
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(
    onGranted: () -> Unit,
    viewModel: PermissionRequestViewModel = viewModel(factory = PermissionRequestViewModel.Factory)
) {
    // State to track whether the permission dialog should be shown
    val requestPermissions by viewModel.requestPermissions.collectAsState()

    // Launcher for requesting permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        viewModel.onGranted(grants)
        onGranted()
    }

    // Decide the starting destination based on the permissions status
    if (requestPermissions) {
        // Show permission request screen
        LaunchedEffect(Unit) {
            permissionLauncher.launch(viewModel.getPermissions()) // Launch permission request
        }
    }
}