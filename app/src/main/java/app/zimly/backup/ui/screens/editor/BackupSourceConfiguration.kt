package app.zimly.backup.ui.screens.editor

import android.net.Uri
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
import app.zimly.backup.ui.theme.containerBackground

@Composable
fun BackupSourceConfiguration(sourceSelector: @Composable (source: SourceType) -> Unit) {

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

        val checked = remember { mutableStateOf(SourceType.MEDIA) }
        val options = mapOf(SourceType.MEDIA to Icons.Outlined.Photo, SourceType.FOLDER to Icons.Outlined.Folder)

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
                        SegmentedButtonDefaults.Icon(active = sourceType == checked.value) {
                            Icon(
                                imageVector = options[sourceType]!!,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    },
                    onCheckedChange = { if (it) checked.value = sourceType },
                    checked = sourceType == checked.value
                ) {
                    Text(sourceType.name)
                }
            }
        }
        sourceSelector(checked.value)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MediaCollectionSelector(
    mediaCollections: Set<String>,
    collection: String,
    select: (collection: String) -> Unit,
    focus: (state: FocusState) -> Unit,
) {

    Column(modifier = Modifier.padding(16.dp)) {

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
                value = collection,
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
}

@Composable
fun DocumentsFolderSelector(
    folder: Uri,
    select: (folder: Uri) -> Unit,
    focus: (state: FocusState) -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                select(uri)
            }
        }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Choose your source directory.")
        Button(
            modifier = Modifier.onFocusChanged { focus(it) },
            onClick = { launcher.launch(null) },
            colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Text(text = "Select Directory")
        }
        Text(text = "Selected directory: $folder")
    }
}
