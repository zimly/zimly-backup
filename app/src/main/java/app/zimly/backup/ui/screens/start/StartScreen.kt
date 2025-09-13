package app.zimly.backup.ui.screens.start

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.zimly.backup.data.db.sync.SyncDirection
import app.zimly.backup.data.media.ContentType
import app.zimly.backup.ui.theme.ZimzyncTheme
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.launch


@Composable
fun StartScreen(
    viewModel: StartViewModel = viewModel(factory = StartViewModel.Factory),
    openSyncProfile: (Int, SyncDirection) -> Unit,
    addSyncProfile: () -> Unit,
) {
    val syncProfiles by viewModel.syncProfilesState.collectAsState(initial = emptyList())
    val numSelected by viewModel.numSelected().collectAsState(0)

    val notification by viewModel.notificationState.collectAsState()

    val snackbarState = remember { SnackbarHostState() }

    StartLayout(
        snackbarState,
        notification,
        clearMessage = { viewModel.clearMessage() },
        addSyncProfile = addSyncProfile,
        numSelected,
        back = { viewModel.resetSelect() },
        copy = { viewModel.viewModelScope.launch { viewModel.copy() } },
        delete = { viewModel.viewModelScope.launch { viewModel.delete() } },
    ) { innerPadding ->
        if (syncProfiles.isEmpty()) {
            GetStarted(innerPadding)
        }
        SyncProfileList(innerPadding, syncProfiles, numSelected, { viewModel.select(it) }, openSyncProfile)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartLayout(
    snackbarState: SnackbarHostState,
    notification: String?,
    clearMessage: () -> Unit,
    addSyncProfile: () -> Unit,
    numSelected: Int,
    back: () -> Unit,
    copy: () -> Unit,
    delete: () -> Unit,
    content: @Composable (innerPadding: PaddingValues) -> Unit
) {

    // Show snackbar when message
    if (notification != null) {
        LaunchedEffect(snackbarState) {
            val result = snackbarState.showSnackbar(
                message = notification,
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.Dismissed -> clearMessage()
                SnackbarResult.ActionPerformed -> clearMessage()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.displayCutout),
        topBar = {
            if (numSelected > 0) {
                TopAppBar(
                    modifier = Modifier.testTag("List Selection Actions"),
                    title = {
                        Text(
                            text = "$numSelected selected",
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
                        IconButton(
                            modifier = Modifier.testTag("Copy Selected"),
                            onClick = { copy() }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CopyAll,
                                contentDescription = "Copy Selected Profiles"
                            )
                        }
                        IconButton(onClick = { delete() }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Selected Profiles"
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    modifier = Modifier.testTag("Zimly Title"),
                    title = {
                        Text(
                            text = "Zimly",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    })
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                addSyncProfile()
            }) {
                Icon(Icons.Filled.Add, "Add Sync Profile")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        })
    { innerPadding ->
        content(innerPadding)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SyncProfileList(
    innerPadding: PaddingValues,
    syncProfiles: List<SyncProfileState>,
    numSelected: Int,
    select: (Int) -> Unit,
    doSync: (Int, SyncDirection) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(all = 16.dp) then Modifier
            .padding(innerPadding)
            .fillMaxWidth()
    ) {
        items(syncProfiles) { syncProfile ->
            SyncProfileListItem(syncProfile, numSelected > 0, select, doSync)
        }
    }
}

@Composable
private fun SyncProfileListItem(
    syncProfile: SyncProfileState,
    selectMode: Boolean,
    select: (Int) -> Unit,
    doSync: (Int, SyncDirection) -> Unit
) {
    Card(
        border = if (syncProfile.selected) BorderStroke(
            2.dp, MaterialTheme.colorScheme.secondary
        ) else null
    ) {
        ListItem(
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        if (selectMode) select(syncProfile.uid) else doSync(
                            syncProfile.uid, syncProfile.direction
                        )
                    },
                    onLongClick = { select(syncProfile.uid) })
                .semantics {
                    onClick("Open configuration", null)
                    onLongClick("Copy or Delete configurations", null)
                },
            headlineContent = { Text(syncProfile.name) },
            supportingContent = {
                Text(
                    syncProfile.url,
                    // https://issuetracker.google.com/issues/305342674
                    // no-wrap as workaround.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = {
                val icon = when (syncProfile.direction) {
                    SyncDirection.UPLOAD -> when (syncProfile.contentType) {
                        ContentType.MEDIA -> Icons.Outlined.Image
                        ContentType.FOLDER -> Icons.Outlined.Folder
                    }

                    SyncDirection.DOWNLOAD -> Icons.Outlined.Cloud
                }
                Icon(
                    icon,
                    "${syncProfile.contentType} ${syncProfile.direction} configuration",
                )
            },
            colors = ListItemDefaults.colors(containerColor = containerBackground())
        )
    }
}

@Composable
private fun GetStarted(innerPadding: PaddingValues) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.padding(all = 16.dp) then Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .offset(y = (-24).dp), // Nudges content upward

        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tap the + button below to create your first backup configuration.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextButton({ uriHandler.openUri("https://www.zimly.app/docs/") }) {
            Text("Get Started Guide")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ZimzyncTheme {
        val syncProfiles = generateSequence(0) { it + 1 }.take(10).map {
            SyncProfileState(
                uid = it,
                name = "test $it",
                url = if (it % 3 == 0) "https://blob.rawbot.zone/$it/very-long-url-that-should-wrap-or-so " else "https://blob.rawbot.zone/$it",
                contentType = if (it % 2 == 0) ContentType.MEDIA else ContentType.FOLDER,
                direction = SyncDirection.UPLOAD
            )
        }.toList()

        val snackbarState = remember { SnackbarHostState() }
        val notification by remember { mutableStateOf<String?>(null) }

        val numSelected = 0
        StartLayout(
            snackbarState,
            notification,
            clearMessage = { },
            addSyncProfile = { },
            numSelected,
            back = { },
            copy = { },
            delete = { },
        ) { innerPadding ->
            if (syncProfiles.isEmpty()) {
                GetStarted(innerPadding)
            }
            SyncProfileList(innerPadding, syncProfiles, numSelected, { }, { _, _ -> })
        }
    }
}

data class SyncProfileState(
    val uid: Int,
    val name: String,
    val url: String,
    val contentType: ContentType,
    val direction: SyncDirection,
    val selected: Boolean = false
)