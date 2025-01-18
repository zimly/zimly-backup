package app.zimly.backup.ui.screens.editor

import android.net.Uri
import app.zimly.backup.data.media.SourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class BackupSourceField(
    private val errorMessage: String = "This field is required."
) {
    val mediaField = Field()
    val folderField = UriField()

    private val internal: MutableStateFlow<SourceType> = MutableStateFlow(SourceType.MEDIA)

    val state: Flow<FieldState> = internal.map {
            return@map FieldState(type = internal.value, collection = mediaField.state.value.value, folder = folderField.state.value.value, errorMessage)
    }

    fun update(value: SourceType) {
        internal.update { value }
    }

    fun isValid(): Boolean {
        return when (internal.value) {
            SourceType.MEDIA -> mediaField.isValid()
            SourceType.FOLDER -> folderField.isValid()
        }
    }

    data class FieldState(val type: SourceType, val collection: String, val folder: Uri, val error: String? = null)


}