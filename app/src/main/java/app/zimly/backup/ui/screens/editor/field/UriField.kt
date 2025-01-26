package app.zimly.backup.ui.screens.editor.field

import android.net.Uri
import androidx.compose.ui.focus.FocusState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UriField(
    errorMessage: String = "This field is required.",
    validate: (value: Uri) -> Boolean = { it.path?.isNotEmpty() ?: false },
    defaultValue: Uri = Uri.EMPTY
): Field<Uri>(errorMessage, validate, defaultValue)