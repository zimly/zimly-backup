package app.zimly.backup.permission

import android.content.ContentResolver
import android.net.Uri

class DocumentsPermissionService {
    companion object {

        fun permissionGranted(contentResolver: ContentResolver, uri: Uri, writePermission: Boolean): Boolean {
                val permissions = contentResolver.persistedUriPermissions
                return permissions.any { it.uri == uri && it.isReadPermission && (!writePermission || it.isWritePermission) }
        }
    }

}