package app.zimly.backup.ui.screens.editor.form.field

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * [Field] is a generic model backing input fields in in the Compose UI.
 * It exposes convenient functions for updating the model from the UI, handling validation
 * and error messages.
 *
 * The internal state is managed in the implementation as e.g. [StateFlow]
 *
 * TODO: Expose a #value(): Flow<T>?
 *
 */
interface Field<T> {
    fun update(value: T)
    fun error(): Flow<String?>
    fun valid(): Flow<Boolean>

    /**
     * Triggers validation of the current state and emits the errorMessage if
     * invalid.
     */
    fun validate()

    /**
     * Sets the touched state on the field. Validation only triggers error message if the field
     * has been touched.
     */
    fun touch()
}

interface FocusableField<T>: Field<T> {
    fun focus(hasFocus: Boolean)
}