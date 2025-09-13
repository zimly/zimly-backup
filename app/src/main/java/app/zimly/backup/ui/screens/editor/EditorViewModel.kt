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
import app.zimly.backup.data.db.sync.SyncProfile
import app.zimly.backup.data.db.sync.SyncDao
import app.zimly.backup.data.db.sync.SyncDirection
import app.zimly.backup.data.media.ContentType
import app.zimly.backup.permission.DocumentsPermissionService
import app.zimly.backup.permission.MediaPermissionService
import app.zimly.backup.ui.components.Notification
import app.zimly.backup.ui.components.NotificationProvider
import app.zimly.backup.ui.screens.editor.EditorViewModel.Companion.SYNC_PROFILE_ID_KEY
import app.zimly.backup.ui.screens.editor.form.BucketForm
import app.zimly.backup.ui.screens.editor.form.field.Permissions
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
 * An overarching [androidx.lifecycle.ViewModel] that keeps an in-memory [Draft] object of the [app.zimly.backup.data.db.sync.SyncProfile] object to be
 * persisted. The final step maps and persists the draft to the DB and the if needed persists the
 * necessary folder permissions. See [bucketStore]s #persist.
 *
 * It's scoped to the wizard navigation graph using [androidx.navigation.NavBackStackEntry].
 */
class EditorViewModel(
    private val syncDao: SyncDao,
    private val contentResolver: ContentResolver,
    private val mediaPermissionService: MediaPermissionService,
    private val syncProfileId: Int? = null
) : ViewModel(), NotificationProvider {

    val draft = MutableStateFlow(Draft())
    val notification: MutableStateFlow<Notification?> = MutableStateFlow(null)

    init {
        syncProfileId?.let {
            viewModelScope.launch {
                val syncProfile = syncDao.loadById(syncProfileId)

                val writePermission = when (syncProfile.direction) {
                    SyncDirection.UPLOAD -> false
                    SyncDirection.DOWNLOAD -> true
                }
                val permissions = when(syncProfile.contentType) {
                    ContentType.MEDIA -> if (mediaPermissionService.permissionsGranted()) Permissions.GRANTED else Permissions.DENIED
                    ContentType.FOLDER -> if (DocumentsPermissionService.permissionGranted(contentResolver, syncProfile.contentUri.toUri(), writePermission)) Permissions.GRANTED else Permissions.DENIED
                }
                draft.value = Draft(
                    bucket = BucketForm.BucketConfiguration(
                        url = syncProfile.url,
                        name = syncProfile.name,
                        key = syncProfile.key,
                        secret = syncProfile.secret,
                        bucket = syncProfile.bucket,
                        region = syncProfile.region,
                        virtualHostedStyle = syncProfile.virtualHostedStyle
                    ),
                    direction = syncProfile.direction,
                    contentType = syncProfile.contentType,
                    contentUri = syncProfile.contentUri,
                    permissions = permissions
                )
            }
        }
    }

    // TODO: Split per usecase: Documents vs Media.
    // Permissions are not used in case of Media. They're handled in [MediaPermissionContainer] standalone.
    val contentStore = object : ValueStore<ContentState> {
        override fun persist(value: ContentState, callback: (Boolean) -> Unit) {
            draft.update {
                it.copy(
                    contentType = value.contentType,
                    contentUri = value.contentUri,
                    permissions = value.permissions
                )
            }
            callback(true)
        }

        override fun load(): Flow<ContentState?> {
            return draft
                .filter { it.contentType != null && it.contentUri != null }
                .map { draft -> ContentState(draft.contentType!!, draft.contentUri!!, draft.permissions) }
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
     * Map the [Draft] object to [app.zimly.backup.data.db.sync.SyncProfile] and persist it.
     */
    suspend fun persist(success: (Boolean) -> Unit) {

        val draftValue = draft.value

        if (draftValue.bucket != null && draftValue.direction != null && draftValue.contentType != null && draftValue.contentUri != null) {

            if (draftValue.contentType == ContentType.FOLDER) {
                persistPermissions(draftValue.direction, draftValue.contentUri)
                draft.update {
                    it.copy(permissions = Permissions.GRANTED)
                }
            }
            val syncProfile = SyncProfile(
                uid = syncProfileId,
                direction = draftValue.direction,
                url = draftValue.bucket.url,
                key = draftValue.bucket.key,
                secret = draftValue.bucket.secret,
                bucket = draftValue.bucket.bucket,
                region = draftValue.bucket.region,
                virtualHostedStyle = draftValue.bucket.virtualHostedStyle,
                name = draftValue.bucket.name,
                contentType = draftValue.contentType,
                contentUri = draftValue.contentUri
            )
            if (syncProfile.uid == null) {
                syncDao.insert(syncProfile)
            } else {
                syncDao.update(syncProfile)
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
        val contentUri: String? = null,
        val permissions: Permissions = Permissions.PENDING
    )

    // Represents the a documents folder or media collection including the permission state.
    // TODO: This should be replaced with [UriPermission] after splitting Media and Documents.
    data class ContentState(
        val contentType: ContentType,
        val contentUri: String,
        val permissions: Permissions
    )

    companion object {
        val TAG: String? = EditorViewModel::class.simpleName

        val SYNC_PROFILE_ID_KEY = object : CreationExtras.Key<Int?> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    checkNotNull(this[APPLICATION_KEY])
                val syncProfileId = this[SYNC_PROFILE_ID_KEY]
                val db = ZimlyDatabase.Companion.getInstance(application.applicationContext)
                val syncDao = db.syncDao()
                val mediaPermissionService = MediaPermissionService(
                    application.applicationContext,
                    application.packageName
                )
                EditorViewModel(syncDao, application.contentResolver, mediaPermissionService, syncProfileId)
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
fun NavController.editorViewModel(syncProfileId: Int?): EditorViewModel {
    val parentEntry = remember(this) {
        getBackStackEntry("wizard")
    }
    // Make sure we null out the value. #getInt return 0 by default, which may be a valid ID

    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = EditorViewModel.Factory,
        extras = MutableCreationExtras().apply {
            this[APPLICATION_KEY] = LocalContext.current.applicationContext as Application
            this[SYNC_PROFILE_ID_KEY] = syncProfileId
        }
    )
}
