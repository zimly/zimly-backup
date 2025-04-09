package app.zimly.backup.ui.screens.editor.field

import kotlinx.coroutines.flow.Flow

interface Field<T> {
    fun update(value: T)
    fun error(): Flow<String?>
    fun valid(): Flow<Boolean>
}