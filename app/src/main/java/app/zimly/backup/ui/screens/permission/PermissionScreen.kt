package app.zimly.backup.ui.screens.permission

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import app.zimly.backup.ui.screens.permission.PermissionViewModel.Companion.getPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PermissionViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(UiState(granted = isPermissionGranted()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Check initially if the permission is granted
    private fun isPermissionGranted(): Boolean {
        val granted = getPermissions()
            .map { permission ->
                ContextCompat.checkSelfPermission(
                    getApplication<Application>().applicationContext,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
            .reduce { granted, permission -> granted && permission }

        return granted
    }

    fun onGranted(grants: Map<String, @JvmSuppressWildcards Boolean>) {
        // Lookup and compare the granted permission
        val granted = checkUserGrants(grants)
        if (granted == true) {
            _state.update { it.copy(granted = true) }
        } else {
            _state.update { it.copy(granted = false, error = "Permissions needed!") }
        }
    }

    data class UiState (
        val granted: Boolean = false,
        val error: String? = null
    )

    companion object {
        /*
         * Returns the needed permissions based on Android SDK version.
         * Changes were introduced to the media permissions in API 33+
         */
        fun getPermissions(): Array<String> {
            val permissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // Compatibility with older versions
                arrayOf(
                    Manifest.permission.ACCESS_MEDIA_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            } else {
                // New permissions
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                )
            }
            return permissions
        }

        fun checkUserGrants(grants: Map<String, Boolean>): Boolean? {
            return getPermissions()
                .map { permission -> grants[permission] }
                .reduce { granted, permission -> granted == true && permission == true }
        }

    }
}

@Composable
fun PermissionScreen(viewModel: PermissionViewModel) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        viewModel.onGranted(grants)
    }

    val state = viewModel.state.collectAsState()

    if (!state.value.granted) {

        state.value.error?.let {
            Text(it)
        }
        if (state.value.error == null) {
            SideEffect {
                permissionLauncher.launch(
                    getPermissions()
                )
            }
        }
    }
}


