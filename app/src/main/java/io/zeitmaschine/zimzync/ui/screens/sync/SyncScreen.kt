package io.zeitmaschine.zimzync.ui.screens.sync

import android.app.Application
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.WorkManager
import io.zeitmaschine.zimzync.data.media.MediaRepository
import io.zeitmaschine.zimzync.data.media.ResolverBasedRepository
import io.zeitmaschine.zimzync.data.remote.RemoteDao
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import io.zeitmaschine.zimzync.ui.theme.containerBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SyncScreen(
    dao: RemoteDao,
    remoteId: Int,
    application: Application,
    edit: (Int) -> Unit,
    back: () -> Unit,
    viewModel: SyncViewModel = viewModel(factory = viewModelFactory {
        initializer {
            val workManager = WorkManager.getInstance(application.applicationContext)
            val mediaRepo: MediaRepository = ResolverBasedRepository(application.contentResolver)
            SyncViewModel(dao, remoteId, workManager, mediaRepo)
        }
    }),
) {

    val remote by viewModel.remoteState.collectAsStateWithLifecycle(SyncViewModel.RemoteState())
    val error by viewModel.error.collectAsStateWithLifecycle()
    val folder by viewModel.folderState.collectAsStateWithLifecycle(SyncViewModel.FolderState())
    val progress by viewModel.progressState.collectAsStateWithLifecycle()

    // want to go nuts?
    // https://afigaliyev.medium.com/snackbar-state-management-best-practices-for-jetpack-compose-1a5963d86d98
    val snackbarState = remember { SnackbarHostState() }

    SyncCompose(
        remote,
        error,
        folder,
        progress,
        snackbarState,
        sync = {
            viewModel.viewModelScope.launch {
                viewModel.sync()
            }
        },
        // Use Dispatchers.Default to not block Main thread
        createDiff = { viewModel.viewModelScope.launch(Dispatchers.Default) { viewModel.createDiff() } },
        edit = { edit(remoteId) },
        back,
        clearError = { viewModel.viewModelScope.launch { viewModel.clearError() } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncCompose(
    remote: SyncViewModel.RemoteState,
    error: String?,
    folder: SyncViewModel.FolderState,
    progress: SyncViewModel.Progress,
    snackbarState: SnackbarHostState,
    sync: () -> Unit,
    createDiff: () -> Unit,
    edit: () -> Unit,
    back: () -> Unit,
    clearError: () -> Unit
) {
    // If the UI state contains an error, show snackbar
    if (!error.isNullOrEmpty()) {
        LaunchedEffect(snackbarState) {
            val result = snackbarState.showSnackbar(
                message = error,
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite
            )
            when (result) {
                SnackbarResult.Dismissed -> clearError()
                SnackbarResult.ActionPerformed -> clearError()
            }

        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        remote.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { back() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { edit() }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Remote"
                        )
                    }

                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }) { innerPadding ->
        Column(
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Bucket(remote)
            Folder(folder)
            Progress(progress)
            val enableActions = progress.status !in setOf(
                SyncViewModel.Status.CALCULATING,
                SyncViewModel.Status.IN_PROGRESS
            )
            Actions(enableActions, createDiff, sync)
        }
    }
}

@Composable
private fun Bucket(remote: SyncViewModel.RemoteState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerBackground()),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Outlined.CloudUpload,
                "Remote",
                modifier = Modifier.padding(top = 8.dp, end = 8.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "URL", textAlign = TextAlign.Left)
                Text(remote.url, textAlign = TextAlign.Right)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Bucket", textAlign = TextAlign.Left)
                Text(remote.bucket)
            }
        }
    }
}

@Composable
private fun Folder(folder: SyncViewModel.FolderState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Outlined.Photo,
                "Media",
                modifier = Modifier.padding(top = 8.dp, end = 8.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Folder")
                Text(text = folder.folder)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Photos")
                Text(text = "${folder.photos}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Videos")
                Text(text = "${folder.videos}")
            }

        }
    }
}

@Composable
private fun Progress(progress: SyncViewModel.Progress) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Outlined.Upload,
                "Progress",
                modifier = Modifier.padding(top = 8.dp, end = 8.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Uploads")
                if (progress.diffCount > -1) {
                    Text(text = "${progress.progressCount} / ${progress.diffCount}")
                } else {
                    Text(text = "-")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Uploads size")
                if (progress.diffBytes > -1) {
                    Text(
                        text = "${
                            Formatter.formatShortFileSize(
                                LocalContext.current,
                                progress.progressBytes
                            )
                        } / ${
                            Formatter.formatShortFileSize(
                                LocalContext.current,
                                progress.diffBytes
                            )
                        }"
                    )
                } else {
                    Text(text = "-")
                }

            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                ProgressBar(progress)
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: SyncViewModel.Progress) {

    val bytesPerSec = remember {
        mutableLongStateOf(-1)
    }
    progress.progressBytesPerSec?.let { bytesPerSec.longValue = it }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {

        if (progress.status == SyncViewModel.Status.IN_PROGRESS && bytesPerSec.longValue > -1) {
            val speed = Formatter.formatShortFileSize(
                LocalContext.current,
                bytesPerSec.longValue
            )
            Text(
                text = "$speed/s",
                fontSize = TextUnit(12F, TextUnitType.Sp),
                fontWeight = FontWeight.Light,
                modifier = Modifier.wrapContentHeight(Alignment.Bottom)
            )
        } else if (progress.status == SyncViewModel.Status.CALCULATING) {
            Text(
                text = "Calculating...",
                fontSize = TextUnit(12F, TextUnitType.Sp),
                fontWeight = FontWeight.Light,
                modifier = Modifier.align(Alignment.Bottom)
            )
        }
    }
    Row {
        if (progress.status == SyncViewModel.Status.CALCULATING) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            )
        } else {
            LinearProgressIndicator(
                progress = { progress.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            )
        }
    }
}

@Composable
private fun Actions(
    enableActions: Boolean,
    createDiff: () -> Unit,
    sync: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                enabled = enableActions,
                onClick = createDiff,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(text = "Calculate")
            }
            Button(
                onClick = sync,
                enabled = enableActions,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Upload")
            }
        }

    }
}


@Preview(showBackground = true)
@Composable
fun InProgressPreview() {

    val remote = SyncViewModel.RemoteState(
        name = "Camera Backup",
        url = "https://my-backup.dyndns.com",
        bucket = "zimly-backup",
        folder = "Camera"
    )
    val progressState = SyncViewModel.Progress(
        status = SyncViewModel.Status.IN_PROGRESS,
        progressBytesPerSec = 36426334,
        percentage = 0.77F,
        diffCount = 51,
        diffBytes = 51*5*1_000_000,
        progressCount = 40,
        progressBytes = 233*1_000_000,
    )
    val folderState = SyncViewModel.FolderState(
        "Camera",
        photos = 3984,
        videos = 273
    )
    val snackbarState = remember { SnackbarHostState() }


    ZimzyncTheme {
        SyncCompose(
            remote = remote,
            error = null,
            folderState,
            progressState,
            sync = {},
            createDiff = {},
            edit = {},
            back = {},
            snackbarState = snackbarState,
            clearError = {}
        )
    }
}
