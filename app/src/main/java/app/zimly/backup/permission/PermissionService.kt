package app.zimly.backup.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionService(private val context: Context) {

    fun isPermissionGranted(): Boolean {
        val granted = getPermissions()
            .map { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
            .reduce { granted, permission -> granted && permission }

        return granted
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
    fun checkUserGrants(grants: Map<String, Boolean>): Boolean? {
        return getPermissions()
            .map { permission -> grants[permission] }
            .reduce { granted, permission -> granted == true && permission == true }
    }

    fun isAnyPermissionPermanentlyDenied(activity: Activity): Boolean {
        return getPermissions().any {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

}