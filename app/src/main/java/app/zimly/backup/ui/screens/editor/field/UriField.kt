package app.zimly.backup.ui.screens.editor.field

import android.net.Uri
import app.zimly.backup.data.media.LocalDocumentsResolver

class UriField(
    errorMessage: String = "This field is required.",
    validate: (value: Uri) -> Boolean = { it.path?.isNotEmpty() ?: false },
    defaultValue: Uri = Uri.EMPTY
): Field<Uri>(errorMessage, validate, defaultValue) {

    // https://stackoverflow.com/questions/17546101/get-real-path-for-uri-android/61995806#61995806
    // What a mess..
    companion object{
        fun displayName(uri: Uri): String {
            return LocalDocumentsResolver.objectPath(uri)
        }
    }
}