package app.zimly.backup.ui.screens.editor.field

import android.net.Uri
import android.provider.DocumentsContract

class UriField(
    errorMessage: String = "This field is required.",
    validate: (value: Uri) -> Boolean = { it.path?.isNotEmpty() ?: false },
    defaultValue: Uri = Uri.EMPTY
): Field<Uri>(errorMessage, validate, defaultValue) {

    // https://stackoverflow.com/questions/17546101/get-real-path-for-uri-android/61995806#61995806
    // What a mess..
    companion object{
        fun displayName(uri: Uri): String? {
            return if (uri == Uri.EMPTY) "" else if (DocumentsContract.isTreeUri(uri)) DocumentsContract.getTreeDocumentId(uri) else uri.path
        }

        fun displayName(uri: String): String? {
            return displayName(Uri.parse(uri))
        }

    }
}