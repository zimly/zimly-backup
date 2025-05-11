package app.zimly.backup.ui.screens.editor.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.zimly.backup.data.db.remote.SyncDirection
import app.zimly.backup.ui.screens.editor.WizardStep
import app.zimly.backup.ui.theme.containerBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDirectionStep(
    store: ValueStore<SyncDirection>, nextStep: (SyncDirection) -> Unit, previousStep: () -> Unit
) {
    val options = listOf(SyncDirection.DOWNLOAD, SyncDirection.UPLOAD)
    var selectedOption by remember { mutableStateOf<SyncDirection?>(store.load()) }

    WizardStep(
        title = "Select Sync Direction",
        navigation = {
            TextButton(onClick = { previousStep() }) {
                Text("Cancel")
            }
            TextButton(
                enabled = selectedOption != null,
                onClick = {
                    selectedOption?.let {
                        store.persist(it)
                        nextStep(it)
                    }
                },
            ) {
                Text("Continue")
            }
        }) {
        options.forEach { option ->
            Card(
                onClick = { selectedOption = option },
                border = if (selectedOption == option) BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.secondary
                ) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {

                val icon = when (option) {
                    SyncDirection.DOWNLOAD -> Icons.Outlined.Cloud
                    SyncDirection.UPLOAD -> Icons.Outlined.Cloud
                }
                ListItem(
                    headlineContent = { Text(option.name) },
                    supportingContent = { Text("Descriptioof Description of Description of $option") },
                    trailingContent = { Icon(icon, "Remote Configuration") },
                    colors = ListItemDefaults.colors(containerColor = containerBackground())
                )
            }
        }
    }
}