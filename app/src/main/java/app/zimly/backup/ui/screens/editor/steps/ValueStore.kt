package app.zimly.backup.ui.screens.editor.steps

import kotlinx.coroutines.flow.Flow

/**
 * Simple interface for loading and storing draft values while stepping through the Wizard.
 */
interface ValueStore<T> {
    fun persist(value: T)
    fun load(): Flow<T?>
}