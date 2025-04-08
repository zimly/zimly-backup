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

class PermissionService(private val context: Context, private val packageName: String) {

    fun isPermissionGranted(): Boolean {
        return getPermissions().all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
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

    /**
     * Takes a map of [Manifest.permission]s and ensures all have been granted.
     */
    fun checkUserGrants(grants: Map<String, Boolean>): Boolean {
        return grants.all { it.value }
    }

    fun isAnyPermissionPermanentlyDenied(activity: Activity): Boolean {
        return getPermissions().any {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }

}