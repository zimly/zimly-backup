package app.zimly.backup.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


@Composable
fun PermissionBox(
    showRational: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Access to your photos and videos is required to complete the media backup configuration.")
        TextButton(
            onClick = { showRational() },
            contentPadding = PaddingValues(
                horizontal = 16.dp,
            ),
        ) {
            Text(text = "Learn More")
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    permissionsPermanentlyDenied: Boolean,
    permissions: Array<String>,
    closeDialog: () -> Unit,
    onGranted: (grants: Map<String, Boolean>) -> Unit,
    openSettings: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        onGranted(grants)
    }
    AlertDialog(
        modifier = Modifier.testTag("permissions_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Missing Permissions Alert",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Permissions Required")
        },
        text = {
            Column {

                Text(
                    text = buildAnnotatedString {
                        append("Zimly requires access to certain permissions to function properly: \n\n")
                        append("  • Access to your media collections\n")
                        append("  • Access to your media's location meta-data (Exif)\n\n")
                        append("Please grant these permissions to enable backup of your photos and videos and ensure your Exif meta-data is preserved.")
                    },
                    textAlign = TextAlign.Left,
                    modifier = Modifier.padding(start = 12.dp)

                )
            }
        },
        onDismissRequest = closeDialog,
        confirmButton = {
            if (permissionsPermanentlyDenied) {
                TextButton(onClick = {
                    openSettings()
                    closeDialog()
                }) {
                    Text("Open Settings")
                }
            } else {
                TextButton(onClick = {
                    permissionLauncher.launch(permissions)
                    closeDialog()
                }) {
                    Text("Grant Permissions")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = closeDialog) {
                Text("Cancel")
            }
        }
    )
}

