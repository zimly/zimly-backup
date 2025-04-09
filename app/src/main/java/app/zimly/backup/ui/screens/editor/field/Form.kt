package app.zimly.backup.ui.screens.editor.field

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class Form(private val fields: List<Field<*>>) {

    fun valid(): Flow<Boolean> = combine(fields.map { it.valid() }) { values ->
        values.all { it }
    }
}
