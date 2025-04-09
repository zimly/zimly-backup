package app.zimly.backup.ui.screens.editor.field

import androidx.compose.runtime.snapshotFlow
import app.zimly.backup.data.media.SourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Composite field for the backup [SourceType] and value that delegates it's logic to the
 * corresponding fields [mediaField] and [folderField].
 */
class BackupSourceField : Field<SourceType> {
    val mediaField = TextField("Select a collection for backup")
    val folderField = UriField("Select a folder for backup")

    private val internal: MutableStateFlow<FieldState> =
        MutableStateFlow(FieldState(SourceType.MEDIA))

    val state: StateFlow<FieldState> = internal.asStateFlow()

    // TODO: Use functions over fields generally?
    override fun error(): Flow<String?> = when (state.value.type) {
        SourceType.MEDIA -> mediaField.error()
        SourceType.FOLDER -> folderField.error()
    }

    override fun update(value: SourceType) {
        internal.update { it.copy(type = value) }
    }

    override fun valid(): Flow<Boolean> = state
        .map { it.type }
        .distinctUntilChanged()
        .flatMapLatest { type ->
            when (type) {
                SourceType.MEDIA -> mediaField.valid()
                SourceType.FOLDER -> folderField.valid()
            }
        }

    fun isValid(): Boolean {
        return when (internal.value.type) {
            SourceType.MEDIA -> mediaField.isValid()
            SourceType.FOLDER -> folderField.isValid()
        }
    }

    data class FieldState(val type: SourceType, val error: String? = null)


}