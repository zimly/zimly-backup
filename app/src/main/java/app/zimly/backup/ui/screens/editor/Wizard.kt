package app.zimly.backup.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.zimly.backup.data.db.remote.SyncDirection
import app.zimly.backup.ui.theme.containerBackground


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Wizard(create: (SyncDirection) -> Unit) {
    val options = listOf(SyncDirection.DOWNLOAD, SyncDirection.UPLOAD)

    Scaffold(
        topBar = { TopAppBar(title = { Text("New Remote Config") }) },
    ) { innerPadding ->

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                Card(
                    onClick = { create(option) },
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
}