package app.zimly.backup.ui.screens.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.ui.screens.editor.field.BackupSourceField
import app.zimly.backup.ui.screens.editor.field.TextField
import app.zimly.backup.ui.screens.editor.field.UriField
import app.zimly.backup.ui.theme.containerBackground

@Composable
fun BackupSourceConfiguration(
    backupSource: BackupSourceField,
    mediaCollections: Set<String>
) {
    // TODO Should this only operate on UI state? If not, should it reset the other option?
    // Or should it go together with the lower onSelect into the parent viewmodel or field?
    val sourceSelector: (type: SourceType) -> Unit = {
        backupSource.update(it)
    }
    val state = backupSource.state.collectAsState()
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

        val options = mapOf(
            SourceType.MEDIA to Icons.Outlined.Photo,
            SourceType.FOLDER to Icons.Outlined.Folder
        )

        MultiChoiceSegmentedButtonRow(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            options.keys.forEachIndexed { index, sourceType ->
                SegmentedButton(
                    colors = SegmentedButtonDefaults.colors(
                        inactiveContainerColor = containerBackground()
                    ),
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = sourceType == state.value.type) {
                            Icon(
                                imageVector = options[sourceType]!!,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    },
                    onCheckedChange = { if (it) sourceSelector(sourceType) },
                    checked = sourceType == state.value.type
                ) {
                    Text(sourceType.name)
                }
            }
        }
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            when (state.value.type) {
                SourceType.MEDIA ->
                    MediaCollectionSelector(mediaCollections, backupSource.mediaField)
                SourceType.FOLDER ->
                    DocumentsFolderSelector(backupSource.folderField)
            }
            BackupSourceError(backupSource)
        }
    }
}

@Composable
private fun BackupSourceError(backupSource: BackupSourceField) {
    val error = backupSource.error().collectAsState(null)
    error.value?.let {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = MaterialTheme.typography.bodySmall.fontSize
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MediaCollectionSelector(
    mediaCollections: Set<String>,
    mediaField: TextField
) {
    val select: (collection: String) -> Unit = {
        mediaField.update(it)
    }
    val focus: (state: FocusState) -> Unit = {
        mediaField.focus(it)
    }
    val collection = mediaField.state.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            // The `menuAnchor` modifier must be passed to the text field for correctness.
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .onFocusChanged { focus(it) },
            readOnly = true,
            value = collection.value.value,
            onValueChange = {},
            label = { Text("Collection") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            mediaCollections.forEach { gallery ->
                DropdownMenuItem(
                    text = { Text(gallery) },
                    onClick = {
                        select(gallery)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun DocumentsFolderSelector(
    folderField: UriField
) {
    val select: (folder: Uri?) -> Unit = { if (it != null) folderField.update(it) else folderField.update(Uri.EMPTY) }
    val focus: () -> Unit = { folderField.touch() }
    val folder = folderField.state.collectAsState()
    val displaySelected = folder.value.value != Uri.EMPTY

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            select(uri)
        }

    if (displaySelected) {
        val displayName = UriField.displayName(folder.value.value)
        OutlinedCard {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Outlined.Folder, contentDescription = "Artist image")
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(displayName)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            onClick = {
                focus()
                launcher.launch(null)
            },
        ) {
            Text(text = if (folder == Uri.EMPTY) "Select Directory" else "Change Directory")
        }
    }
}
