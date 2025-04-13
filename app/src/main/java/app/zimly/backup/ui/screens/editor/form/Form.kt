package app.zimly.backup.ui.screens.editor.form

import kotlinx.coroutines.flow.Flow

/**
 * A form groups [Field]s into logical collection.
 */
interface Form {
    fun valid(): Flow<Boolean>
    fun validate()
}