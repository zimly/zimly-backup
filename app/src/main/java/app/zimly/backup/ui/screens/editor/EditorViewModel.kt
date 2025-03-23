package app.zimly.backup.ui.screens.editor

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.zimly.backup.data.db.remote.Remote
import app.zimly.backup.data.db.remote.RemoteDao
import app.zimly.backup.data.media.LocalMediaRepository
import app.zimly.backup.data.media.MediaRepository
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.data.s3.MinioRepository
import app.zimly.backup.ui.screens.editor.field.BackupSourceField
import app.zimly.backup.ui.screens.editor.field.TextField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class EditorViewModel(application: Application, private val dao: RemoteDao, remoteId: Int?) :
    AndroidViewModel(application) {

    companion object {
        val TAG: String? = EditorViewModel::class.simpleName
    }

    private val contentResolver by lazy { application.contentResolver }
    private val mediaRepo: MediaRepository = LocalMediaRepository(contentResolver)

    // https://stackoverflow.com/questions/69689843/jetpack-compose-state-hoisting-previews-and-viewmodels-best-practices
    // TODO ???? https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate
    // Internal mutable state
    private val internal: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    // Expose Ui State
    val state: StateFlow<UiState> = internal.asStateFlow()

    val name = TextField()
    val url = TextField(
        errorMessage = "Not a valid URL.",
        validate = { URLUtil.isValidUrl(it) })
    val key = TextField()
    val secret = TextField()
    val bucket = TextField()
    val backupSource = BackupSourceField()


    init {
        try {
            // TODO push this down into own VM in MediaCollectionSelector?
            val collections = mediaRepo.getBuckets().keys
            internal.update {
                it.copy(
                    mediaCollections = collections,
                    title = "New configuration"
                )
            }
        } catch (e: Exception) {
            val errorMessage = "Failed to load Media Collections: ${e.message}"
            Log.e(TAG, errorMessage, e)

            internal.update {
                it.copy(
                    title = "New configuration",
                    notificationError = true,
                    notification = errorMessage
                )
            }
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

                backupSource.update(remote.sourceType)
                when (remote.sourceType) {
                    SourceType.MEDIA -> backupSource.mediaField.update(remote.sourceUri)
                    SourceType.FOLDER -> backupSource.folderField.update(remote.sourceUri.toUri())
                }
            }
        }
    }

    suspend fun save(success: () -> Unit) {

        if (isValid()) {

            val sourceType = backupSource.state.value.type
            val sourceUri = when (sourceType) {
                SourceType.MEDIA -> backupSource.mediaField.state.value.value
                SourceType.FOLDER -> backupSource.folderField.state.value.value.toString()
            }
            if (sourceType == SourceType.FOLDER) {
                persistPermissions(backupSource.folderField.state.value.value)
            }
            val remote = Remote(
                internal.value.uid,
                name.state.value.value,
                url.state.value.value,
                key.state.value.value,
                secret.state.value.value,
                bucket.state.value.value,
                sourceType,
                sourceUri,
            )
            if (remote.uid == null) {
                dao.insert(remote)
            } else {
                dao.update(remote)
            }
            success()
        } else {
            internal.update { it.copy(notification = "Form has errors, won't save.", notificationError = true) }
        }
    }

    private fun isValid(): Boolean {
        return name.isValid() && url.isValid() && key.isValid() && secret.isValid() && bucket.isValid() && backupSource.isValid()
    }

    private fun persistPermissions(folder: Uri) {
        contentResolver.takePersistableUriPermission(folder, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun clearSnackbar() {
        internal.update { it.copy(notification = null, notificationError = false) }
    }

    suspend fun verify() {
        if (isValid()) {
            val url = url.state.value.value
            val key = key.state.value.value
            val secret = secret.state.value.value
            val bucket = bucket.state.value.value
            val repo = MinioRepository(url, key, secret, bucket)

            try {
                val bucketExists = repo.bucketExists()
                val message = if (bucketExists) "Connection successful, bucket exists!" else "Bucket does not exist!"
                internal.update { it.copy(notification = message) }
            } catch (e: Exception) {
                internal.update { it.copy(notification = "Connection failed: $e", notificationError = true) }
            }
        } else {
            internal.update { it.copy(notification = "Form has errors, can't connect.", notificationError = true) }
        }
    }


    data class UiState(
        var uid: Int? = null,
        var title: String = "",
        var mediaCollections: Set<String> = emptySet(),
        var notificationError: Boolean = false,
        var notification: String? = null,
    )

}