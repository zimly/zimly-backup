package app.zimly.backup.ui.screens.permission

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

@Composable
fun PermissionScreen(onGranted: () -> Unit) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->

        // TODO ViewModel?
        // Lookup and compare the granted permission
        val granted = getPermissions()
            .map { permission -> isGranted[permission] }
            .reduce { granted, permission -> granted == true && permission == true }

        if (granted == true) {
            Log.i("PermissionScreen", "Permissions granted")
            onGranted()
        } else {
            // TODO Implement some sort of informative screen, that the user
            // needs to grant permissions for the app to work.
            Log.i("PermissionScreen", "PERMISSION DENIED")
        }
    }
    SideEffect {
        permissionLauncher.launch(
            getPermissions()
        )
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