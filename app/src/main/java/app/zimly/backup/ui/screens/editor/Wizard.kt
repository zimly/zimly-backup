package app.zimly.backup.ui.screens.editor

import android.annotation.SuppressLint
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
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
class WizardViewModel(private val remoteDao: RemoteDao, private val contentResolver: ContentResolver) : ViewModel() {

    var draft = Draft()

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

        if (draft.contentType== ContentType.FOLDER) {
            persistPermissions(draft.direction!!, draft.contentUri!!)
        }
        val remote = Remote(
            uid = null,
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
        remoteDao.insert(remote)
    }

    private fun persistPermissions(direction: SyncDirection, uri: String) {
        val modeFlags = when(direction) {
            SyncDirection.UPLOAD -> Intent.FLAG_GRANT_READ_URI_PERMISSION
            SyncDirection.DOWNLOAD -> Intent.FLAG_GRANT_READ_URI_PERMISSION and Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                val db = ZimlyDatabase.getInstance(application.applicationContext)
                val remoteDao = db.remoteDao()

                WizardViewModel(remoteDao, application.contentResolver)
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
    navigation: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {

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
fun NavController.wizardViewModel(): WizardViewModel {
    val parentEntry = remember(this) {
        getBackStackEntry("wizard")
    }

    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = WizardViewModel.Factory
    )
}

