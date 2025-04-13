package app.zimly.backup.ui.screens.editor.form.field

import android.net.Uri
import app.zimly.backup.data.media.LocalDocumentsResolver

class UriField(
    errorMessage: String = "This field is required.",
    validate: (value: Uri) -> Boolean = { it.path?.isNotEmpty() ?: false },
    defaultValue: Uri = Uri.EMPTY
): BaseField<Uri>(errorMessage, validate, defaultValue) {

    companion object {
        fun displayName(uri: Uri): String {
            return LocalDocumentsResolver.objectPath(uri)
        }
    }
}