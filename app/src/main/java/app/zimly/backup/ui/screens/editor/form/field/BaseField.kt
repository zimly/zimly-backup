package app.zimly.backup.ui.screens.editor.form.field

import androidx.compose.ui.focus.FocusState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A base implementation for [Field]s that exposes [StateFlow] similar to a ViewModel for views.
 */
abstract class BaseField<T>(
    private val errorMessage: String = "This field is required.",
    private val validate: (value: T) -> Boolean,
    defaultValue: T,
    initialValidation: Boolean = false
): Field<T> {
    private var touched: Boolean? = null
    private var internal: MutableStateFlow<FieldState<T>> = MutableStateFlow(FieldState(defaultValue, initialValidation))
    val state: StateFlow<FieldState<T>> = internal.asStateFlow()

    override fun update(value: T) {
        internalUpdate(value)
    }

    override fun validate() {
        internalUpdate(internal.value.value)
    }

    /**
     * Takes care of updating the internal state in one go, meaning there's only one emit on the
     * state [Flow]s. If the value update and validation updates happen separately, there would be
     * 2 emits, which affects the test assertions and the UI render cycles as well.
     */
    private fun internalUpdate(value: T) {
        val valid = validate(value)
        val showError = !valid && this.touched == true
        internal.update {
            it.copy(
                value,
                valid = valid,
                error = if (showError) errorMessage else null // setting this to null is important!
            )
        }
    }

    override fun error(): Flow<String?> = state.map { it.error }
    override fun valid(): Flow<Boolean> = state.map { it.valid }

    /**
     * Handle [FocusState] changes on the field. The idea behind this is to not show errors
     * on first touch of the field, but rather when moving on to the next field.
     *
     * TODO: Move to a FocusableField interface?
     */
    fun focus(focus: FocusState) {
        if (touched == null && focus.hasFocus) {
            this.touched = false
        } else if (touched == false && !focus.hasFocus) {
            this.touched = true
        }
        validate()
    }

    override fun touch() {
        this.touched = true
    }

    data class FieldState<T>(val value: T, val valid: Boolean = false, val error: String? = null)
}