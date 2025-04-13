package app.zimly.backup.ui.screens.editor.form.field

import app.zimly.backup.data.media.SourceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Composite field for the backup [SourceType] and value that delegates it's logic to the
 * corresponding fields [mediaField] and [folderField].
 *
 * This is borderline a form, similar to [BucketForm]. But with more complex logic and as it represents
 * _one_ value in the model a composite field is better.
 */
class BackupSourceField : Field<SourceType> {
    val mediaField = TextField("Select a collection for backup")
    val folderField = UriField("Select a folder for backup")

    private val internal: MutableStateFlow<FieldState> =
        MutableStateFlow(FieldState(SourceType.MEDIA))

    val state: StateFlow<FieldState> = internal.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun error(): Flow<String?> = state
        .map { it.type }
        .flatMapLatest { type ->
            when (type) {
                SourceType.MEDIA -> mediaField.error()
                SourceType.FOLDER -> folderField.error()
            }
        }


    override fun update(value: SourceType) {
        internal.update { it.copy(type = value) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun valid(): Flow<Boolean> = state
        .map { it.type }
        .flatMapLatest { type ->
            when (type) {
                SourceType.MEDIA -> mediaField.valid()
                SourceType.FOLDER -> folderField.valid()
            }
        }

    override fun validate() {
        touch()
        mediaField.validate()
        folderField.validate()
    }

    override fun touch() {
        mediaField.touch()
        folderField.touch()
    }

    data class FieldState(val type: SourceType, val error: String? = null)

}