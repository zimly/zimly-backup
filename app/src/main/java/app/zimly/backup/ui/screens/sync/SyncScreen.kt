package app.zimly.backup.ui.screens.sync

import android.app.Application
import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CloudUpload
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
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
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
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.data.remote.RemoteDao
import app.zimly.backup.ui.theme.ZimzyncTheme
import app.zimly.backup.ui.theme.containerBackground
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
            SyncViewModel(dao, remoteId, workManager, application.contentResolver)
        }
    }),
) {

    val remote by viewModel.syncConfigurationState.collectAsStateWithLifecycle(SyncViewModel.SyncConfigurationState())
    val error by viewModel.error.collectAsStateWithLifecycle()
    val progress by viewModel.progressState.collectAsStateWithLifecycle()

    // want to go nuts?
    // https://afigaliyev.medium.com/snackbar-state-management-best-practices-for-jetpack-compose-1a5963d86d98
    val snackbarState = remember { SnackbarHostState() }

    val source = @Composable {
        when(remote.sourceType) {
            SourceType.MEDIA -> MediaCollectionCompose(remote.sourceUri, application)
            SourceType.FOLDER -> DocumentsFolderCompose(remote.sourceUri, application)
            null -> {}
        }
    }
    SyncCompose(
        remote,
        error,
        source,
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
    remote: SyncViewModel.SyncConfigurationState,
    error: String?,
    source: @Composable () -> Unit,
    progress: SyncViewModel.Progress,
    snackbarState: SnackbarHostState,
    sync: () -> Unit,
    createDiff: () -> Unit,
    edit: () -> Unit,
    back: () -> Unit,
    clearError: () -> Unit
) {
    val enableActions = progress.status !in setOf(
        SyncViewModel.Status.CALCULATING,
        SyncViewModel.Status.IN_PROGRESS
    )
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = sync,
                    enabled = enableActions,
                    contentPadding = PaddingValues(horizontal = 74.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Text(text = "Upload")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }) { innerPadding ->
        Column(
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Bucket(remote)
            source()
            DiffDetails(progress, enableActions, createDiff)
            progress.status?.let { ProgressBar(progress) }
        }
    }
}

@Composable
private fun Bucket(remote: SyncViewModel.SyncConfigurationState) {
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
private fun DiffDetails(
    progress: SyncViewModel.Progress,
    enableDiff: Boolean,
    createDiff: () -> Unit,
) {
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
        }
        Row(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center // Arrangement.Absolute.Right
        ) {

            TextButton(
                enabled = enableDiff,
                onClick = createDiff,
                contentPadding = PaddingValues(horizontal = 24.dp), // Reset padding
                modifier = Modifier
                    .height(32.dp),
            ) {
                Text(text = "Calculate")
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {

            val text = when (progress.status) {
                SyncViewModel.Status.CALCULATING -> "Calculating"
                SyncViewModel.Status.COMPLETED -> "Completed"
                SyncViewModel.Status.IN_PROGRESS -> if (bytesPerSec.longValue > 0)
                    "${
                        Formatter.formatShortFileSize(
                            LocalContext.current,
                            bytesPerSec.longValue
                        )
                    }/s" else ""

                null -> ""
            }
            Text(
                text = text,
                fontSize = TextUnit(12F, TextUnitType.Sp),
                fontWeight = FontWeight.Light,
                modifier = Modifier.wrapContentHeight(Alignment.Bottom)
            )
        }

        Row {
            if (progress.status == SyncViewModel.Status.CALCULATING) {
                LinearProgressIndicator(
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            } else {
                val animatedProgress by
                animateFloatAsState(
                    label = "Animated Progress",
                    targetValue = progress.percentage,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}

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


    ZimzyncTheme(darkTheme = true) {
        SyncCompose(
            remote = remote,
            error = null,
            {},
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


    ZimzyncTheme(darkTheme = true) {
        SyncCompose(
            remote = remote,
            error = null,
            { },
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


    ZimzyncTheme(darkTheme = true) {
        SyncCompose(
            remote = remote,
            error = null,
            {  },
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


    ZimzyncTheme(darkTheme = true) {
        SyncCompose(
            remote = remote,
            error = null,
            { },
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
