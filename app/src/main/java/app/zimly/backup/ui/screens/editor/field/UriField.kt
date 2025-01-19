package app.zimly.backup.ui.screens.editor.field

import android.net.Uri
import androidx.compose.ui.focus.FocusState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UriField(
    private val errorMessage: String = "This field is required.",
    private val validate: (value: Uri) -> Boolean = { it.path?.isNotEmpty() ?: false },
) {
    private var touched: Boolean? = null
    private val internal: MutableStateFlow<FieldState> = MutableStateFlow(FieldState())
    val state: StateFlow<FieldState> = internal.asStateFlow()

    fun update(value: Uri) {
        internal.update {
            it.copy(
                value = value,
                error = if (isError()) errorMessage else null
            )
        }
    }

    fun focus(focus: FocusState) {
        if (touched == null && focus.hasFocus) {
            touched = false
        } else if (touched == false && !focus.hasFocus) {
            touched = true
        }
        if (isError()) {
            internal.update {
                it.copy(
                    error = errorMessage
                )
            }
        }
    }

    private fun isError(): Boolean {
        return touched == true && !isValid()
    }

    fun isValid(): Boolean {
        return validate(internal.value.value)
    }


    data class FieldState(val value: Uri = Uri.EMPTY, val error: String? = null)
}