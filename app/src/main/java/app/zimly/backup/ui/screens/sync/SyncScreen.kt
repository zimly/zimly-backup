package app.zimly.backup.ui.screens.sync

import android.app.Application
import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import app.zimly.backup.data.db.remote.SyncDirection
import app.zimly.backup.data.media.ContentType
import app.zimly.backup.ui.screens.sync.battery.BatterySaverContainer
import app.zimly.backup.ui.screens.sync.permission.MediaPermissionContainer
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SyncScreen(
    remoteId: Int,
    edit: (SyncDirection?, Int) -> Unit,
    back: () -> Unit,
    viewModel: SyncViewModel = viewModel(
        factory = SyncViewModel.Factory,
        extras = MutableCreationExtras().apply {
            set(APPLICATION_KEY, LocalContext.current.applicationContext as Application)
            set(SyncViewModel.REMOTE_ID_KEY, remoteId)
        })
) {

    val syncConfigurationState by viewModel.syncConfigurationState.collectAsStateWithLifecycle(null)
    val error by viewModel.error.collectAsStateWithLifecycle()
    val progress by viewModel.progressState.collectAsStateWithLifecycle()
    val permissionsGranted by viewModel.permissionsGranted.collectAsStateWithLifecycle()
    val syncInProgress by viewModel.syncInProgress.collectAsStateWithLifecycle()

    // want to go nuts?
    // https://afigaliyev.medium.com/snackbar-state-management-best-practices-for-jetpack-compose-1a5963d86d98
    val snackbarState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Use Dispatchers.Default to not block Main thread
    val createDiff: () -> Unit =
        {
            viewModel.viewModelScope.launch(Dispatchers.Default) {
                val safeContext = context.applicationContext
                viewModel.createDiff(safeContext)
            }
        }

    syncConfigurationState?.let { syncConfiguration ->
        SyncLayout(
            syncConfiguration,
            error,
            permissionsGranted,
            syncInProgress,
            snackbarState,
            sync = {
                viewModel.viewModelScope.launch {
                    viewModel.sync()
                }
            },
            cancelSync = { viewModel.cancelSync() },
            edit = { edit(syncConfiguration.direction, remoteId) },
            back,
            clearError = { viewModel.viewModelScope.launch { viewModel.clearError() } },
        ) {
            val enableDiffAction = permissionsGranted && !syncInProgress

            SyncOverview(
                syncConfiguration,
                progress,
                enableDiffAction,
                createDiff,
                sourceContainer = {
                    when (syncConfiguration.contentType) {
                        ContentType.MEDIA -> MediaCollectionContainer(syncConfiguration.sourceUri)
                        ContentType.FOLDER -> DocumentsFolderContainer(syncConfiguration.sourceUri)
                    }
                },
                warningsContainer = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MediaPermissionContainer()
                        BatterySaverContainer()
                    }
                }
            )

        }
    }
}

@Composable
fun SyncOverview(
    remote: SyncViewModel.SyncConfigurationState,
    progress: SyncViewModel.Progress,
    enableActions: Boolean,
    createDiff: () -> Unit,
    sourceContainer: @Composable () -> Unit,
    warningsContainer: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Bucket(remote)
        sourceContainer()
        DiffDetails(progress, enableActions, createDiff, remote.direction)

    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        progress.status?.let { ProgressBar(progress) }

        warningsContainer()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncLayout(
    syncConfiguration: SyncViewModel.SyncConfigurationState,
    error: String?,
    enableActions: Boolean,
    syncInProgress: Boolean,
    snackbarState: SnackbarHostState,
    sync: () -> Unit,
    cancelSync: () -> Unit,
    edit: () -> Unit,
    back: () -> Unit,
    clearError: () -> Unit,
    content: @Composable () -> Unit
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
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.displayCutout),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        syncConfiguration.name,
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
                    .padding(bottom = 32.dp, top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (!syncInProgress) {
                    Button(
                        enabled = enableActions,
                        onClick = sync,
                        contentPadding = PaddingValues(horizontal = 74.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        val label = when(syncConfiguration.direction) {
                            SyncDirection.UPLOAD -> "Upload"
                            SyncDirection.DOWNLOAD -> "Download"
                        }
                        Text(text = label)
                    }

                } else {
                    Button(
                        enabled = enableActions,
                        onClick = cancelSync,
                        contentPadding = PaddingValues(horizontal = 74.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                    ) {
                        Text(text = "Cancel")
                    }
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
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            content()
        }
    }
}

@Composable
private fun Bucket(remote: SyncViewModel.SyncConfigurationState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerBackground()),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = when(remote.direction) {
                    SyncDirection.UPLOAD -> "S3 Upload Target"
                    SyncDirection.DOWNLOAD -> "S3 Download Source"
                }
            }
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            val icon = when(remote.direction) {
                SyncDirection.UPLOAD -> Icons.Outlined.CloudUpload
                SyncDirection.DOWNLOAD -> Icons.Outlined.CloudDownload
            }
            Icon(
                icon,
                "Remote",
                modifier = Modifier
                    .semantics { hideFromAccessibility() }
                    .padding(top = 8.dp, end = 8.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "URL", textAlign = TextAlign.Left)
                Text(remote.url, textAlign = TextAlign.Right)
            }
            remote.region?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Region", textAlign = TextAlign.Left)
                    Text(remote.region!!)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
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
    direction: SyncDirection,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = when(direction) {
                    SyncDirection.UPLOAD -> "Upload Diff Details"
                    SyncDirection.DOWNLOAD -> "Download Diff Details"
                }
            }
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            val icon = when(direction) {
                SyncDirection.UPLOAD -> Icons.Outlined.Upload
                SyncDirection.DOWNLOAD -> Icons.Outlined.Download
            }
            Icon(
                icon,
                "Progress",
                modifier = Modifier
                    .padding(top = 8.dp, end = 8.dp)
                    .semantics { hideFromAccessibility() }
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val label = when(direction) {
                    SyncDirection.UPLOAD -> "Uploads"
                    SyncDirection.DOWNLOAD -> "Downloads"
                }
                Text(text = label)
                if (progress.diffCount > -1) {
                    Text(text = "${progress.progressCount} / ${progress.diffCount}")
                } else {
                    Text(text = "-", modifier = Modifier.semantics { contentDescription = "Not yet calculated" })
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val label = when(direction) {
                    SyncDirection.UPLOAD -> "Uploads"
                    SyncDirection.DOWNLOAD -> "Downloads"
                }
                Text(text = "$label size")
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
                    Text(text = "-",  modifier = Modifier.semantics { contentDescription = "Not yet calculated" })
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
                    .height(32.dp)
                    .semantics { onClick(label = "Calculate Diff", null) },
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
            .fillMaxHeight(),
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
                SyncViewModel.Status.CONSTRAINTS_NOT_MET -> "Waiting for network"
                SyncViewModel.Status.WAITING -> "Waiting"
                SyncViewModel.Status.CALCULATING -> "Calculating"
                SyncViewModel.Status.COMPLETED -> "Completed"
                SyncViewModel.Status.CANCELLED -> "Cancelled"
                SyncViewModel.Status.FAILED -> "Failed"
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
            val launchingStates = setOf(
                SyncViewModel.Status.CALCULATING,
                SyncViewModel.Status.WAITING,
                SyncViewModel.Status.CONSTRAINTS_NOT_MET
            )
            when (progress.status) {
                in launchingStates -> {
                    LinearProgressIndicator(
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                SyncViewModel.Status.IN_PROGRESS -> {
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

                else -> {
                    LinearProgressIndicator(
                        progress = { 0.0F },
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
