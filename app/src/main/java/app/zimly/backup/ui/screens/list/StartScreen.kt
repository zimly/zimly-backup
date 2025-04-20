package app.zimly.backup.ui.screens.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.zimly.backup.R
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.ui.theme.ZimzyncTheme
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.launch


@Composable
fun StartScreen(
    // https://programmer.ink/think/a-new-way-to-create-a-viewmodel-creationextras.html
    viewModel: StartViewModel = viewModel(factory = StartViewModel.Factory),
    syncRemote: (Int) -> Unit,
    addRemote: () -> Unit,
) {
    val remotes by viewModel.remotesState.collectAsState(initial = emptyList())
    val notification by viewModel.notificationState.collectAsState()

    val snackbarState = remember { SnackbarHostState() }

    StartLayout(
        remotes = remotes,
        snackbarState,
        notification,
        clearMessage = { viewModel.clearMessage() },
        syncRemote = syncRemote,
        addRemote = addRemote,
        select = { viewModel.select(it) },
        back = { viewModel.resetSelect() },
        copy = { viewModel.viewModelScope.launch { viewModel.copy() } },
        delete = { viewModel.viewModelScope.launch { viewModel.delete() } },
        numSelected = viewModel.numSelected()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartLayout(
    remotes: List<RemoteView>,
    snackbarState: SnackbarHostState,
    notification: String?,
    clearMessage: () -> Unit,
    syncRemote: (Int) -> Unit,
    addRemote: () -> Unit,
    select: (Int) -> Unit,
    back: () -> Unit,
    copy: () -> Unit,
    delete: () -> Unit,
    numSelected: Int
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
        topBar = {
            if (numSelected > 0) {
                TopAppBar(
                    title = {
                        Text(
                            text = "$numSelected selected ",
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
                        IconButton(onClick = { copy() }) {
                            Icon(
                                imageVector = Icons.Filled.CopyAll,
                                contentDescription = "Copy Selected Remotes"
                            )
                        }
                        IconButton(onClick = { delete() }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Copy Selected Remotes"
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
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
                addRemote()
            }) {
                Icon(Icons.Filled.Add, "Add Remote")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        })
    { innerPadding ->
        if (remotes.isEmpty()) {
            GetStarter(innerPadding)
        }
        RemoteList(innerPadding, remotes, numSelected, select, syncRemote)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RemoteList(
    innerPadding: PaddingValues,
    remotes: List<RemoteView>,
    numSelected: Int,
    select: (Int) -> Unit,
    syncRemote: (Int) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding()
        ) then Modifier.fillMaxWidth()
    ) {
        items(remotes) { remote ->
            Box(
                modifier = Modifier
                    // Note: Order matters!
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = containerBackground())
                    .then(
                        if (remote.selected) Modifier.border(
                            width = Dp(2f),
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(12.dp)
                        ) else Modifier
                    )
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (numSelected > 0) select(remote.uid) else syncRemote(
                                remote.uid
                            )
                        },
                        onLongClick = { select(remote.uid) })
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(remote.name, color = MaterialTheme.colorScheme.onSurface)
                        Text(remote.url, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(modifier = Modifier.wrapContentWidth()) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            val icon = when (remote.sourceType) {
                                SourceType.MEDIA -> Icons.Outlined.Image
                                SourceType.FOLDER -> Icons.Outlined.Folder
                            }
                            Icon(icon, "Remote Configuration")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GetStarter(innerPadding: PaddingValues) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding()
        ) then Modifier
            .fillMaxSize()
            .offset(y = (-24).dp), // Nudges content upward

        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.zimly_logo),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
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
        val remotes = generateSequence(0) { it + 1 }.take(10).map {
            RemoteView(
                uid = it,
                name = "test $it",
                url = "https://blob.rawbot.zone/$it",
                sourceType = if (it % 2 == 0) SourceType.MEDIA else SourceType.FOLDER
            )
        }.toList()

        val snackbarState = remember { SnackbarHostState() }
        val notification by remember { mutableStateOf<String?>(null) }

        StartLayout(
            remotes = remotes,
            snackbarState,
            notification,
            {},
            syncRemote = {},
            addRemote = {},
            {},
            {},
            {},
            {},
            0
        )
    }
}

data class RemoteView(
    val uid: Int,
    val name: String,
    val url: String,
    val sourceType: SourceType,
    val selected: Boolean = false
)