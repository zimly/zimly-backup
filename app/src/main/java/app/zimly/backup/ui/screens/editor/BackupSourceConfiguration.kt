package app.zimly.backup.ui.screens.editor

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.ui.theme.containerBackground

@Composable
fun BackupSourceConfiguration(state: State<EditorViewModel.UiState>, source: SourceField) {

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

        val checked = remember { mutableStateOf("Media") }
        val options = mapOf("Media" to Icons.Outlined.Photo, "Files" to Icons.Outlined.Folder)

        MultiChoiceSegmentedButtonRow(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            options.keys.forEachIndexed { index, label ->
                SegmentedButton(
                    colors = SegmentedButtonDefaults.colors(
                        inactiveContainerColor = containerBackground()
                    ),
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = label == checked.value) {
                            Icon(
                                imageVector = options[label]!!,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    },
                    onCheckedChange = { if (it) checked.value = label },
                    checked = label == checked.value
                ) {
                    Text(label)
                }
            }
        }
        if (checked.value == "Media") {
            MediaCollectionSelector(state, source)
        } else {
            DocumentsFolderSelector({ uri -> Log.i("SKR", uri.path.toString()) }, { Log.i("SKR", "cancel") })
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MediaCollectionSelector(
    state: State<EditorViewModel.UiState>,
    collectionField: SourceField
) {

    Column(modifier = Modifier.padding(16.dp)) {

        var expanded by remember { mutableStateOf(false) }
        val folderState = collectionField.state.collectAsState()

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                // The `menuAnchor` modifier must be passed to the text field for correctness.
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .onFocusChanged { collectionField.focus(it) },
                readOnly = true,
                value = folderState.value.value.second,
                onValueChange = {},
                label = { Text("Collection") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                state.value.galleries.forEach { gallery ->
                    DropdownMenuItem(
                        text = { Text(gallery) },
                        onClick = {
                            collectionField.update(Pair(SourceType.MEDIA, gallery))
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentsFolderSelector(
    onFolderSelected: (Uri) -> Unit,
    onCancelled: () -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                onFolderSelected(uri)
            } else {
                onCancelled()
            }
        }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Choose your source directory.")
        Button(
            onClick = { launcher.launch(null) },
            colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Text(text = "Select Directory")
        }
    }
}
