package app.zimly.backup.ui.screens.editor

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.db.ZimlyDatabase
import app.zimly.backup.data.db.remote.Remote
import app.zimly.backup.data.db.remote.RemoteDao
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.data.s3.MinioRepository
import app.zimly.backup.ui.screens.editor.field.BackupSourceField
import app.zimly.backup.ui.screens.editor.field.RegionField
import app.zimly.backup.ui.screens.editor.field.TextField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    private val contentResolver: ContentResolver,
    private val dao: RemoteDao,
    remoteId: Int?
) : ViewModel() {

    companion object {
        val TAG: String? = EditorViewModel::class.simpleName

        // Optional remote ID
        val REMOTE_ID_KEY = object : CreationExtras.Key<Int?> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                val remoteId = this[REMOTE_ID_KEY]
                val db = ZimlyDatabase.getInstance(application.applicationContext)
                val remoteDao = db.remoteDao()

                val contentResolver = application.contentResolver
                EditorViewModel(contentResolver, remoteDao, remoteId)
            }
        }
    }

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
    val region = RegionField()
    val backupSource = BackupSourceField()

    init {
        try {
            internal.update {
                it.copy(
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
                region.update(remote.region)

                backupSource.update(remote.sourceType)
                when (remote.sourceType) {
                    SourceType.MEDIA -> backupSource.mediaField.update(remote.sourceUri)
                    SourceType.FOLDER -> backupSource.folderField.update(remote.sourceUri.toUri())
                }
            }
        }
    }

    suspend fun save(success: () -> Unit) {

        if (formValid()) {

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
                region.state.value.value,
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
            internal.update {
                it.copy(
                    notification = "Form has errors, won't save.",
                    notificationError = true
                )
            }
        }
    }

    private fun formValid(): Boolean {
        return bucketValid() && backupSource.isValid()
    }

    private fun bucketValid() =
        name.isValid() && url.isValid() && key.isValid() && secret.isValid() && bucket.isValid()

    private fun persistPermissions(folder: Uri) {
        contentResolver.takePersistableUriPermission(folder, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun clearSnackbar() {
        internal.update { it.copy(notification = null, notificationError = false) }
    }

    suspend fun verify() {
        if (bucketValid()) {
            val url = url.state.value.value
            val key = key.state.value.value
            val secret = secret.state.value.value
            val bucket = bucket.state.value.value
            val region = region.state.value.value
            val repo = MinioRepository(url, key, secret, bucket, region)

            try {
                val bucketExists = repo.bucketExists()
                val message =
                    if (bucketExists) "Connection successful, bucket exists!" else "Bucket does not exist!"
                internal.update { it.copy(notification = message) }
            } catch (e: Exception) {
                internal.update {
                    it.copy(
                        notification = "Connection failed: $e",
                        notificationError = true
                    )
                }
            }
        } else {
            internal.update {
                it.copy(
                    notification = "Bucket configuration has errors, can't connect.",
                    notificationError = true
                )
            }
        }
    }


    data class UiState(
        var uid: Int? = null,
        var title: String = "",
        var notificationError: Boolean = false,
        var notification: String? = null,
    )

}