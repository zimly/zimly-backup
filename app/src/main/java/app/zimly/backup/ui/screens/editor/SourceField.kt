package app.zimly.backup.ui.screens.editor

import androidx.compose.ui.focus.FocusState
import app.zimly.backup.data.media.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SourceField(
    private val errorMessage: String = "This field is required.",
    private val validate: (value: Pair<SourceType, String>) -> Boolean = { it.first in SourceType.entries.toTypedArray() && it.second.isNotEmpty()},
) {
    private var touched: Boolean? = null
    private val internal: MutableStateFlow<FieldState> = MutableStateFlow(FieldState(value = Pair(SourceType.MEDIA, "")))
    val state: StateFlow<FieldState> = internal.asStateFlow()

    fun update(value: Pair<SourceType, String>) {
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


    data class FieldState(val value: Pair<SourceType, String>, val error: String? = null)
}