package app.zimly.backup.ui.screens.editor

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import app.zimly.backup.ui.components.NotificationBar
import app.zimly.backup.ui.components.NotificationProvider
import app.zimly.backup.ui.screens.editor.WizardViewModel.Companion.REMOTE_ID_KEY
import app.zimly.backup.ui.screens.editor.form.BucketForm
import app.zimly.backup.ui.screens.editor.steps.ValueStore
import kotlinx.coroutines.launch


/**
 * An overarching [ViewModel] that keeps an in-memory [Draft] object of the [Remote] object to be
 * persisted. The final step maps and persists the draft to the DB and the if needed persists the
 * necessary folder permissions. See [bucketStore]s #persist.
 *
 * It's scoped to the wizard navigation graph using [NavBackStackEntry].
 */
class WizardViewModel(
    private val remoteDao: RemoteDao,
    private val contentResolver: ContentResolver,
    private val remoteId: Int? = null
) : ViewModel() {

    var draft = Draft()

    init {
        remoteId?.let {
            viewModelScope.launch {
                val remote = remoteDao.loadById(remoteId)
                draft = Draft(
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
        override fun persist(value: Pair<ContentType, String>) {
            draft = draft.copy(
                contentType = value.first,
                contentUri = value.second
            )
        }

        override fun load(): Pair<ContentType, String>? {
            if (draft.contentType != null && draft.contentUri != null)
                return Pair(draft.contentType!!, draft.contentUri!!)
            return null
        }
    }

    val directionStore = object : ValueStore<SyncDirection> {
        override fun persist(value: SyncDirection) {
            draft = draft.copy(direction = value)
        }

        override fun load(): SyncDirection? {
            return draft.direction
        }
    }

    val bucketStore = object : ValueStore<BucketForm.BucketConfiguration> {
        override fun persist(value: BucketForm.BucketConfiguration) {
            draft = draft.copy(bucket = value)

            viewModelScope.launch { persist() }
        }

        override fun load(): BucketForm.BucketConfiguration? {
            return draft.bucket
        }
    }

    /**
     * Map the [Draft] object to [Remote] and persist it.
     */
    suspend fun persist() {

        if (draft.contentType == ContentType.FOLDER) {
            persistPermissions(draft.direction!!, draft.contentUri!!)
        }
        val remote = Remote(
            uid = remoteId,
            direction = draft.direction!!,
            url = draft.bucket!!.url,
            key = draft.bucket!!.key,
            secret = draft.bucket!!.secret,
            bucket = draft.bucket!!.bucket,
            region = draft.bucket!!.region,
            name = draft.bucket!!.name,
            contentType = draft.contentType!!,
            contentUri = draft.contentUri!!
        )
        if (remote.uid == null) {
            remoteDao.insert(remote)
        } else {
            remoteDao.update(remote)
        }
    }

    private fun persistPermissions(direction: SyncDirection, uri: String) {
        val modeFlags = when (direction) {
            SyncDirection.UPLOAD -> Intent.FLAG_GRANT_READ_URI_PERMISSION
            SyncDirection.DOWNLOAD -> Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        contentResolver.takePersistableUriPermission(uri.toUri(), modeFlags)
    }

    data class Draft(
        val direction: SyncDirection? = null,
        val bucket: BucketForm.BucketConfiguration? = null,
        val contentType: ContentType? = null,
        val contentUri: String? = null
    )

    companion object {
        val TAG: String? = WizardViewModel::class.simpleName

        val REMOTE_ID_KEY = object : CreationExtras.Key<Int?> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                val remoteId = this[REMOTE_ID_KEY]
                val db = ZimlyDatabase.getInstance(application.applicationContext)
                val remoteDao = db.remoteDao()

                WizardViewModel(remoteDao, application.contentResolver, remoteId)
            }
        }
    }

}

/**
 * Provides the chrome for the individual wizard steps.
 *
 * TODO: Implement error handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardStep(
    title: String,
    notificationHost: NotificationProvider? = null,
    navigation: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {

    notificationHost?.let{
        NotificationBar(it)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.SpaceBetween
            ) {
                navigation()
            }
        }
    ) { innerPadding ->

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier.fillMaxWidth()
        ) {
            content()
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
fun NavController.wizardViewModel(remoteId: Int?): WizardViewModel {
    val parentEntry = remember(this) {
        getBackStackEntry("wizard?id={id}")
    }
    // Make sure we null out the value. #getInt return 0 by default, which may be a valid ID

    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = WizardViewModel.Factory,
        extras = MutableCreationExtras().apply {
            this[APPLICATION_KEY] = LocalContext.current.applicationContext as Application
            this[REMOTE_ID_KEY] = remoteId
        }
    )
}
