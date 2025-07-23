package app.zimly.backup.permission

import android.content.ContentResolver
import androidx.core.net.toUri
import app.zimly.backup.data.db.remote.SyncDirection
import app.zimly.backup.ui.screens.sync.SyncViewModel.SyncConfigurationState

class DocumentsPermissionService {
    companion object {
        // TODO: Do not user SyncConfigurationState here
        fun permissionGranted(contentResolver: ContentResolver, remote: SyncConfigurationState): Boolean {
                val permissions = contentResolver.persistedUriPermissions
                val readOnly = remote.direction == SyncDirection.UPLOAD
                return permissions.any { it.uri == remote.contentUri.toUri() && it.isReadPermission && (readOnly || it.isWritePermission) }
        }
    }

}