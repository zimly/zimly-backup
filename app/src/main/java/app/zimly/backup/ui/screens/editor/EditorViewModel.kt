package app.zimly.backup.ui.screens.editor

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import app.zimly.backup.data.db.ZimlyDatabase
import app.zimly.backup.data.db.remote.Remote
import app.zimly.backup.data.db.remote.RemoteDao
import app.zimly.backup.data.db.remote.SyncDirection
import app.zimly.backup.data.media.ContentType
import app.zimly.backup.ui.components.Notification
import app.zimly.backup.ui.components.NotificationProvider
import app.zimly.backup.ui.screens.editor.EditorViewModel.Companion.REMOTE_ID_KEY
import app.zimly.backup.ui.screens.editor.form.BucketForm
import app.zimly.backup.ui.screens.editor.steps.ValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * An overarching [androidx.lifecycle.ViewModel] that keeps an in-memory [Draft] object of the [app.zimly.backup.data.db.remote.Remote] object to be
 * persisted. The final step maps and persists the draft to the DB and the if needed persists the
 * necessary folder permissions. See [bucketStore]s #persist.
 *
 * It's scoped to the wizard navigation graph using [androidx.navigation.NavBackStackEntry].
 */
class EditorViewModel(
    private val remoteDao: RemoteDao,
    private val contentResolver: ContentResolver,
    private val remoteId: Int? = null
) : ViewModel(), NotificationProvider {

    var draft = MutableStateFlow(Draft())
    val notification: MutableStateFlow<Notification?> = MutableStateFlow(null)

    init {
        remoteId?.let {
            viewModelScope.launch {
                val remote = remoteDao.loadById(remoteId)
                draft.value = Draft(
                    bucket = BucketForm.BucketConfiguration(
                        url = remote.url,
                        name = remote.name,
                        key = remote.key,
                        secret = remote.secret,
                        bucket = remote.bucket,
                        region = remote.region,
                    ),
                    direction = remote.direction,
                    contentType = remote.contentType,
                    contentUri = remote.contentUri
                )
            }
        }
    }

    val contentStore = object : ValueStore<Pair<ContentType, String>> {
        override fun persist(value: Pair<ContentType, String>, callback: (Boolean) -> Unit) {
            draft.update {
                it.copy(
                    contentType = value.first,
                    contentUri = value.second
                )
            }
            callback(true)
        }

        override fun load(): Flow<Pair<ContentType, String>?> {
            return draft
                .filter { it.contentType != null && it.contentUri != null }
                .map { draft -> Pair(draft.contentType!!, draft.contentUri!!) }
        }
    }

    val directionStore = object : ValueStore<SyncDirection> {
        override fun persist(value: SyncDirection, callback: (Boolean) -> Unit) {
            draft.update {
                it.copy(direction = value)
            }
            callback(true)
        }

        override fun load(): Flow<SyncDirection?> {
            return draft.map { it.direction }
        }
    }

    val bucketStore = object : ValueStore<BucketForm.BucketConfiguration> {
        override fun persist(value: BucketForm.BucketConfiguration, callback: (Boolean) -> Unit) {
            draft.update {
                it.copy(bucket = value)
            }
            viewModelScope.launch { persist(callback) }

        }

        override fun load(): Flow<BucketForm.BucketConfiguration?> {
            return draft.map { it.bucket }
        }
    }

    /**
     * Map the [Draft] object to [app.zimly.backup.data.db.remote.Remote] and persist it.
     */
    suspend fun persist(success: (Boolean) -> Unit) {

        val draftValue = draft.value

        if (draftValue.bucket != null && draftValue.direction != null && draftValue.contentType != null && draftValue.contentUri != null) {

            if (draftValue.contentType == ContentType.FOLDER) {
                persistPermissions(draftValue.direction, draftValue.contentUri)
            }
            val remote = Remote(
                uid = remoteId,
                direction = draftValue.direction,
                url = draftValue.bucket.url,
                key = draftValue.bucket.key,
                secret = draftValue.bucket.secret,
                bucket = draftValue.bucket.bucket,
                region = draftValue.bucket.region,
                name = draftValue.bucket.name,
                contentType = draftValue.contentType,
                contentUri = draftValue.contentUri
            )
            if (remote.uid == null) {
                remoteDao.insert(remote)
            } else {
                remoteDao.update(remote)
            }
            success(true)
        } else {
            notification.update {
                Notification(
                    message = "Validation failed, cannot save configuration.",
                    type = Notification.Type.ERROR
                )
            }
            success(false)
        }
    }

    private fun persistPermissions(direction: SyncDirection, uri: String) {
        val modeFlags = when (direction) {
            SyncDirection.UPLOAD -> Intent.FLAG_GRANT_READ_URI_PERMISSION
            SyncDirection.DOWNLOAD -> Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        contentResolver.takePersistableUriPermission(uri.toUri(), modeFlags)
    }

    override fun reset() {
        notification.update { null }
    }

    override fun get(): StateFlow<Notification?> {
        return notification.asStateFlow()
    }

    data class Draft(
        val direction: SyncDirection? = null,
        val bucket: BucketForm.BucketConfiguration? = null,
        val contentType: ContentType? = null,
        val contentUri: String? = null
    )

    companion object {
        val TAG: String? = EditorViewModel::class.simpleName

        val REMOTE_ID_KEY = object : CreationExtras.Key<Int?> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY])
                val remoteId = this[REMOTE_ID_KEY]
                val db = ZimlyDatabase.Companion.getInstance(application.applicationContext)
                val remoteDao = db.remoteDao()

                EditorViewModel(remoteDao, application.contentResolver, remoteId)
            }
        }
    }

}

/**
 * Creates a shareable ViewModel for the individual wizard steps, that's scoped to the parent
 * [NavBackStackEntry].
 * The linting error is a false negative it seems.
 */
@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun NavController.editorViewModel(remoteId: Int?): EditorViewModel {
    val parentEntry = remember(this) {
        getBackStackEntry("wizard")
    }
    // Make sure we null out the value. #getInt return 0 by default, which may be a valid ID

    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = EditorViewModel.Factory,
        extras = MutableCreationExtras().apply {
            this[APPLICATION_KEY] = LocalContext.current.applicationContext as Application
            this[REMOTE_ID_KEY] = remoteId
        }
    )
}
