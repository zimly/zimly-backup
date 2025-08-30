package app.zimly.backup.ui.screens.editor.form.field

import android.net.Uri
import app.zimly.backup.data.media.LocalDocumentsResolver

class UriField(
    errorMessage: String = "This field is required.",
    validate: (value: UriPermission) -> Boolean = { !it.uri.path.isNullOrEmpty() && it.permission != Permissions.DENIED },
    defaultValue: UriPermission = UriPermission()
): BaseField<UriPermission>(errorMessage, validate, defaultValue) {

    companion object {
        fun displayName(uri: Uri): String {
            return LocalDocumentsResolver.objectPath(uri)
        }
    }
}

data class UriPermission (
    val uri: Uri = Uri.EMPTY,
    val permission: Permissions = Permissions.DENIED
)

// Document Permissions are granted upon persisting the draft to the DB, hence there's a pending state.
enum class Permissions {
    GRANTED, DENIED, PENDING //, PERSISTED?
}
