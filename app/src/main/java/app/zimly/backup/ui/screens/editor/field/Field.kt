package app.zimly.backup.ui.screens.editor.field

import androidx.compose.ui.focus.FocusState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A field holds the business logic and state representation of an input field. Similar to a
 * ViewModel for views.
 *
 * Validation and error state are triggered by [FocusState] changes and value updates.
 */
abstract class Field<T>(
    private val errorMessage: String = "This field is required.",
    private val validate: (value: T) -> Boolean,
    defaultValue: T
) {
    private var touched: Boolean? = null
    private var internal: MutableStateFlow<FieldState<T>> = MutableStateFlow(FieldState(defaultValue))
    val state: StateFlow<FieldState<T>> = internal.asStateFlow()

    fun update(value: T) {
        val error = touched != null && !validate(value)
        internal.update {
            it.copy(
                value = value,
                error = if (error) errorMessage else null
            )
        }
    }

    fun error(): Flow<String?> = state.map { it.error }

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

    fun touch() {
        this.touched = true
    }

    private fun isError(): Boolean {
        return touched == true && !isValid()
    }

    fun isValid(): Boolean {
        return validate(internal.value.value)
    }


    data class FieldState<T>(val value: T, val error: String? = null)
}