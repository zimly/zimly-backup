package app.zimly.backup.ui.screens.permission

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.ZimlyApplication
import app.zimly.backup.ui.screens.permission.PermissionViewModel.Companion.getPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Handles the permission requests needed for the app to function properly. Instead of handling
 * permissions globally in this screen and blocking the app in case of missing permissions, we might
 * want to request the permissions where they are used, and provide a degraded user experience in case
 * of missing permissions.
 *
 * @see [Android Permission Requests](https://developer.android.com/training/permissions/requesting)
 */
class PermissionViewModel(private val application: Application, private val closeActivity: () -> Unit) :
    AndroidViewModel(application) {

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

    fun onGranted(grants: Map<String, Boolean>) {

        // Lookup and compare the granted permission
        val granted = checkUserGrants(grants)
        if (granted == true) {
            _state.update { it.copy(granted = true) }
        } else {
            _state.update { it.copy(granted = false, error = true, grants = grants) }
        }
    }

    fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", getApplication<ZimlyApplication>().packageName, null)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        application.applicationContext.startActivity(intent)
        // Don't send user back to permission-screen
        closeActivity()
    }

    data class UiState(
        val granted: Boolean = false,
        val error: Boolean = false,
        val grants: Map<String, Boolean> = emptyMap()
    )

    companion object {
        val CALLBACK_KEY = object : CreationExtras.Key<() -> Unit> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ZimlyApplication
                val callBack = this[CALLBACK_KEY] as () -> Unit

                PermissionViewModel(application, callBack)
            }
        }

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

    Scaffold(
        bottomBar = {
            if (state.value.error) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { viewModel.openSettings() },
                        contentPadding = PaddingValues(horizontal = 74.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Text("Update Permissions")
                    }
                }
            }
        }
    ) { innerPadding ->

        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            if (!state.value.granted) {

                if (state.value.grants.isNotEmpty()) {
                    PermissionsMissing(state.value.grants)
                }
                if (!state.value.error) {
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(
                            getPermissions()
                        )
                    }
                }
            }

        }
    }

}

@Composable
fun PermissionsMissing(grants: Map<String, Boolean>) {
    Scaffold(
        bottomBar = {
        }
    ) { innerPadding ->

        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Missing Permissions!",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    modifier = Modifier.padding(7.dp)
                )
                Text(
                    "Zimly needs the following permissions to function properly",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(7.dp)
                )
                grants.map {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = it.key.substringAfterLast("."))
                        if (it.value) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                "Permission check"
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Error,
                                "Permission fail",
                            )
                        }
                    }
                }
            }
        }
    }
}


