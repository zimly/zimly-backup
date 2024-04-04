package io.zeitmaschine.zimzync.ui.screens.editor

import android.app.Application
import android.webkit.URLUtil
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.zeitmaschine.zimzync.data.media.MediaRepository
import io.zeitmaschine.zimzync.data.remote.Remote
import io.zeitmaschine.zimzync.data.remote.RemoteDao
import io.zeitmaschine.zimzync.data.media.ResolverBasedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(application: Application, private val dao: RemoteDao, remoteId: Int?) :
    AndroidViewModel(application) {

    private val contentResolver by lazy { application.contentResolver }
    private val mediaRepo: MediaRepository = ResolverBasedRepository(contentResolver)

    // https://stackoverflow.com/questions/69689843/jetpack-compose-state-hoisting-previews-and-viewmodels-best-practices
    // TODO ???? https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate
    // Internal mutable state
    private val internal: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    // Expose Ui State
    val state: StateFlow<UiState> = internal.asStateFlow()

    val name: Field = Field()
    val url: Field = Field(
        errorMessage = "Not a valid URL.",
        validate = { URLUtil.isValidUrl(it) })
    val key: Field = Field()
    val secret: Field = Field()
    val bucket: Field = Field()
    val folder: Field = Field(errorMessage = "Select a media gallery to synchronize.")


    init {
        val galleries = mediaRepo.getBuckets().keys
        internal.update {
            it.copy(
                galleries = galleries,
                title = "New configuration"
            )
        }

        remoteId?.let {
            viewModelScope.launch {
                val remote = dao.loadById(remoteId)

                internal.update {
                    it.copy(
                        uid = remote.uid,
                        title = remote.name,
                    )
                }
                name.update(remote.name)
                url.update(remote.url)
                key.update(remote.key)
                secret.update(remote.secret)
                bucket.update(remote.bucket)
                folder.update(remote.folder)

            }
        }
    }

    suspend fun save(success: () -> Unit) {
        val valid =
            name.isValid() && url.isValid() && key.isValid() && secret.isValid() && bucket.isValid() && folder.isValid()
        if (valid) {
            val remote = Remote(
                internal.value.uid,
                name.state.value.value, // TODO from state vs internal state?
                url.state.value.value,
                key.state.value.value,
                secret.state.value.value,
                bucket.state.value.value,
                folder.state.value.value,
            )
            if (remote.uid == null) {
                dao.insert(remote)
            } else {
                dao.update(remote)
            }
            success()
        } else {
            internal.update { it.copy(error = "Form has errors, won't save.") }
        }
    }

    fun clearError() {
        internal.update { it.copy(error = "") }
    }


    data class UiState(
        var uid: Int? = null,
        var title: String = "",
        var galleries: Set<String> = emptySet(),
        var error: String = "",
    )

}