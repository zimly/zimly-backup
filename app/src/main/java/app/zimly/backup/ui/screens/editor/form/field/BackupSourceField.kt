package app.zimly.backup.ui.screens.editor.form.field

import app.zimly.backup.data.media.ContentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Composite field for the backup [ContentType] and value that delegates it's logic to the
 * corresponding fields [mediaField] and [folderField].
 *
 * This should most likely be a [app.zimly.backup.ui.screens.editor.form.Form].
 */
class BackupSourceField : Field<ContentType> {
    val mediaField = TextField("Select a collection for backup")
    val folderField = UriField("Select a folder and grant permissions for your data")

    private val internal: MutableStateFlow<FieldState<ContentType>> =
        MutableStateFlow(FieldState(ContentType.MEDIA))

    override val state: StateFlow<FieldState<ContentType>> = internal.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun error(): Flow<String?> = state
        .map { it.value }
        .flatMapLatest { type ->
            when (type) {
                ContentType.MEDIA -> mediaField.error()
                ContentType.FOLDER -> folderField.error()
            }
        }

    override fun update(value: ContentType) {
        internal.update { it.copy(value = value) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun valid(): Flow<Boolean> = state
        .map { it.value }
        .flatMapLatest { type ->
            when (type) {
                ContentType.MEDIA -> mediaField.valid()
                ContentType.FOLDER -> folderField.valid()
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

}