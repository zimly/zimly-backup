package app.zimly.backup.ui.screens.sync

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import app.zimly.backup.data.db.notification.Notification
import app.zimly.backup.data.db.notification.NotificationDao
import app.zimly.backup.data.db.notification.NotificationType
import app.zimly.backup.data.media.ContentObject
import app.zimly.backup.data.media.LocalContentResolver
import app.zimly.backup.data.media.LocalMediaResolver
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.ui.screens.sync.battery.BatterySaverContainer
import app.zimly.backup.ui.screens.sync.battery.BatterySaverViewModel
import app.zimly.backup.ui.screens.sync.battery.PowerStatusProvider
import app.zimly.backup.ui.theme.ZimzyncTheme
import java.io.InputStream

@Preview(showBackground = true)
@Composable
fun InProgressPreview() {

    val remote = SyncViewModel.SyncConfigurationState(
        name = "Camera Backup",
        url = "https://minio.zimly.cloud",
        bucket = "2024-Camera",
        sourceType = SourceType.MEDIA,
        sourceUri = "Camera"
    )
    val progressState = SyncViewModel.Progress(
        status = SyncViewModel.Status.IN_PROGRESS,
        progressBytesPerSec = 18426334,
        percentage = 0.77F,
        diffCount = 51,
        diffBytes = 51 * 5 * 1_000_000,
        progressCount = 40,
        progressBytes = 233 * 1_000_000,
    )
    val snackbarState = remember { SnackbarHostState() }

    val enableActions = progressState.status !in setOf(
        SyncViewModel.Status.CALCULATING,
        SyncViewModel.Status.IN_PROGRESS
    )

    PreviewSync(remote, enableActions, snackbarState, progressState)
}

@Preview(showBackground = true)
@Composable
fun CompletedPreview() {

    val remote = SyncViewModel.SyncConfigurationState(
        name = "Camera Backup",
        url = "https://my-backup.dyndns.com",
        bucket = "zimly-backup",
        sourceType = SourceType.MEDIA,
        sourceUri = "Camera"
    )
    val progressState = SyncViewModel.Progress(
        status = SyncViewModel.Status.COMPLETED,
        progressBytesPerSec = 18426334,
        percentage = 1F,
        diffCount = 51,
        diffBytes = 51 * 5 * 1_000_000,
        progressCount = 51,
        progressBytes = 51 * 5 * 1_000_000,
    )
    val snackbarState = remember { SnackbarHostState() }

    val enableActions = progressState.status !in setOf(
        SyncViewModel.Status.CALCULATING,
        SyncViewModel.Status.IN_PROGRESS
    )

    PreviewSync(remote, enableActions, snackbarState, progressState, true)
}

@Preview(showBackground = true)
@Composable
fun IdlePreview() {

    val remote = SyncViewModel.SyncConfigurationState(
        name = "Camera Backup",
        url = "https://my-backup.dyndns.com",
        sourceType = SourceType.MEDIA,
        sourceUri = "Camera"
    )
    val progressState = SyncViewModel.Progress()
    val snackbarState = remember { SnackbarHostState() }

    val enableActions = progressState.status !in setOf(
        SyncViewModel.Status.CALCULATING,
        SyncViewModel.Status.IN_PROGRESS
    )

    PreviewSync(remote, enableActions, snackbarState, progressState)}


@Preview(showBackground = true)
@Composable
fun CalculatingPreview() {

    val remote = SyncViewModel.SyncConfigurationState(
        name = "Camera Backup",
        url = "https://minio.zimly.cloud",
        bucket = "2024-Camera",
        sourceType = SourceType.MEDIA,
        sourceUri = "Camera"
    )
    val progressState = SyncViewModel.Progress(status = SyncViewModel.Status.CALCULATING)

    val snackbarState = remember { SnackbarHostState() }

    val enableActions = progressState.status !in setOf(
        SyncViewModel.Status.CALCULATING,
        SyncViewModel.Status.IN_PROGRESS
    )

    PreviewSync(remote, enableActions, snackbarState, progressState)}

@Composable
private fun PreviewSync(
    remote: SyncViewModel.SyncConfigurationState,
    enableActions: Boolean,
    snackbarState: SnackbarHostState,
    progressState: SyncViewModel.Progress,
    batteryWarning: Boolean = false
) {
    ZimzyncTheme(darkTheme = true) {
        SyncLayout(
            remoteName = remote.name,
            error = null,
            enableActions,
            sync = {},
            cancelSync = {},
            edit = {},
            back = {},
            snackbarState = snackbarState,
            clearError = {},
        ) {
            SyncOverview(
                remote,
                progressState,
                enableActions,
                createDiff = {},
                sourceContainer = { ContentContainer(remote) },
                batterySaverContainer = {
                    BatterySaverContainer(viewModel = viewModel {
                        val stubNotificationDao = object: NotificationDao {
                            override suspend fun loadByType(type: NotificationType): Notification? { return null }
                            override suspend fun update(notification: Notification) {}
                            override suspend fun insert(notification: Notification) {}
                        }
                        BatterySaverViewModel(StubPowerStatusProvider(!batteryWarning), stubNotificationDao)
                    })
                }
            )
        }
    }
}

@Composable
private fun ContentContainer(remote: SyncViewModel.SyncConfigurationState) {
    when (remote.sourceType) {
        SourceType.MEDIA -> {
            MediaCollectionContainer(remote.sourceUri, viewModel = viewModel {
                MediaCollectionViewModel(StubMediaResolver(), remote.sourceUri)
            })
        }

        SourceType.FOLDER -> {
            DocumentsFolderContainer(remote.sourceUri, viewModel = viewModel {
                DocumentsFolderViewModel(
                    StubContentResolver(),
                    Uri.parse(remote.sourceUri)
                )
            })
        }

        null -> {}
    }

}

private class StubContentResolver : LocalContentResolver {

    override fun getStream(uri: Uri): InputStream {
        TODO("Not yet implemented")
    }

    override fun listObjects(): List<ContentObject> {
        return listOf(
            ContentObject(
                "name",
                124L,
                "image/png",
                Uri.EMPTY
            )
        )
    }
}


private class StubMediaResolver : LocalMediaResolver, LocalContentResolver {
    override fun photoCount(): Int {
        return 23
    }

    override fun videoCount(): Int {
        return 12
    }

    override fun getStream(uri: Uri): InputStream {
        TODO("Not yet implemented")
    }

    override fun listObjects(): List<ContentObject> {
        return listOf(ContentObject("name", 124L, "image/png", Uri.EMPTY))
    }
}

private class StubPowerStatusProvider(private val disabled: Boolean) : PowerStatusProvider {
    override fun isCharging(): Boolean {
        return false
    }

    override fun isBatterSaverDisabled(): Boolean {
        return disabled
    }

}