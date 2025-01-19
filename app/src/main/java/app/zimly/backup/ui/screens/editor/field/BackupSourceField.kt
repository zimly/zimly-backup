package app.zimly.backup.ui.screens.editor.field

import app.zimly.backup.data.media.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BackupSourceField(
    private val errorMessage: String = "This field is required."
) {
    val mediaField = Field()
    val folderField = UriField()

    private val internal: MutableStateFlow<FieldState> = MutableStateFlow(FieldState(SourceType.MEDIA))

    val state: StateFlow<FieldState> = internal.asStateFlow()

    fun update(value: SourceType) {
        internal.update { it.copy(type = value) }
    }

    fun isValid(): Boolean {
        return when (internal.value.type) {
            SourceType.MEDIA -> mediaField.isValid()
            SourceType.FOLDER -> folderField.isValid()
        }
    }

    data class FieldState(val type: SourceType, val error: String? = null)


}