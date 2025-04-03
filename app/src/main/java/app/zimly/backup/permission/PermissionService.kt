package app.zimly.backup.permission

import android.Manifest
import android.os.Build

class PermissionService {

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
}