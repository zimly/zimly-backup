package app.zimly.backup.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MediaPermissionService(private val context: Context, private val packageName: String, permissionsProvider: () -> Array<String> = ::permissionProvider) {

    private val requiredPermissions = permissionsProvider()

    fun permissionsGranted(): Boolean {
        return requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Verifies whether any of the permissions were permanently denied. This means the dialog
     * won't open any more and the user needs to go through app settings manually.
     */
    fun permissionsDenied(activity: Activity): Boolean {
        return requiredPermissions().any {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

    fun requiredPermissions(): Array<String> {
        return requiredPermissions
    }

    /**
     * Takes a map of [Manifest.permission]s and ensures all have been granted.
     */
    fun verifyGrants(grants: Map<String, Boolean>): Boolean {
        return grants.all { it.value }
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }
}

/**
 * Provides the required permissions based on Android SDK version.
 * Changes were introduced to the media permissions in API 33+
 */
fun permissionProvider(): Array<String> {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // Compatibility with older versions
        arrayOf(
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
    }
}
