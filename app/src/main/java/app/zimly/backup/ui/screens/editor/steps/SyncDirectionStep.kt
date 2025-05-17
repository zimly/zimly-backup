package app.zimly.backup.ui.screens.editor.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.zimly.backup.data.db.remote.SyncDirection
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDirectionStep(
    store: ValueStore<SyncDirection>, nextStep: (SyncDirection) -> Unit, previousStep: () -> Unit
) {
    val loadedOption by store.load().collectAsStateWithLifecycle(null)
    var selectedOption by remember { mutableStateOf(loadedOption) }
    WizardStep(
        title = "Select Sync Direction",
        navigation = {
            TextButton(onClick = { previousStep() }) {
                Text("Cancel")
            }
            TextButton(
                enabled = selectedOption != null,
                onClick = {
                    selectedOption?.let { selected ->
                        store.persist(selected) { nextStep(selected) }
                    }
                },
            ) {
                Text("Continue")
            }
        }) {

        SyncOption(
            selected = selectedOption == SyncDirection.UPLOAD,
            option = SyncDirection.UPLOAD,
            onSelect = { selectedOption = it },
            title = "Upload from Device",
            description = "Synchronize media or documents from your device to a remote S3 bucket",
            icon = Icons.Outlined.Upload
        )
        SyncOption(
            selected = selectedOption == SyncDirection.DOWNLOAD,
            option = SyncDirection.DOWNLOAD,
            onSelect = { selectedOption = it },
            title = "Download from S3",
            description = "Synchronize remote data to your mobile device",
            icon = Icons.Outlined.CloudDownload
        )
    }
}

@Composable
private fun SyncOption(
    selected: Boolean = false,
    option: SyncDirection,
    onSelect: (SyncDirection) -> Unit,
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        onClick = { onSelect(option) },
        border = if (selected) BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.secondary
        ) else null
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(description) },
            trailingContent = { Icon(icon, title) },
            colors = ListItemDefaults.colors(containerColor = containerBackground())
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SyncDirectionStepPreview() {

    val stubStore: ValueStore<SyncDirection> = object : ValueStore<SyncDirection> {

        var direction: SyncDirection? = null
        override fun persist(value: SyncDirection, callback: (Boolean) -> Unit) {
            direction = value
        }

        override fun load(): Flow<SyncDirection?> = flow { direction }

    }
    SyncDirectionStep(
        stubStore,
        nextStep = {},
        previousStep = {}
    )
}
