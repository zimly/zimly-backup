package app.zimly.backup.ui.screens.editor.field

import android.net.Uri

class UriField(
    errorMessage: String = "This field is required.",
    validate: (value: Uri) -> Boolean = { it.path?.isNotEmpty() ?: false },
    defaultValue: Uri = Uri.EMPTY
): Field<Uri>(errorMessage, validate, defaultValue)