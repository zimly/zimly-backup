package app.zimly.backup.ui.screens.sync.permission

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.permission.DocumentsPermissionService
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DocumentsPermissionViewModel(
    private val contentResolver: ContentResolver,
    private val targetUri: Uri,
    private val writePermission: Boolean
) : ViewModel() {

    private val _showWarning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showWarning: StateFlow<Boolean> = _showWarning.asStateFlow()

    init {
        updateState()
    }

    fun updateState() {
        _showWarning.value = !DocumentsPermissionService.permissionGranted(contentResolver, targetUri, writePermission)
    }

    companion object {

        val FOLDER_URI = object : CreationExtras.Key<Uri> {}
        val WRITE_PERMISSION = object : CreationExtras.Key<Boolean> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                val uri = checkNotNull(this[FOLDER_URI])
                val writePermission = checkNotNull(this[WRITE_PERMISSION])
                DocumentsPermissionViewModel(application.contentResolver, uri, writePermission)
            }
        }
    }
}

@Composable
fun DocumentsPermissionContainer(
    edit: () -> Unit,
    folderPath: String,
    writePermission: Boolean,
    viewModel: DocumentsPermissionViewModel = viewModel(
        factory = DocumentsPermissionViewModel.Factory,
        extras = MutableCreationExtras().apply {
            set(APPLICATION_KEY, LocalContext.current.applicationContext as Application)
            set(DocumentsPermissionViewModel.FOLDER_URI, folderPath.toUri())
            set(DocumentsPermissionViewModel.WRITE_PERMISSION, writePermission)
        })
) {

    val showWarning by viewModel.showWarning.collectAsStateWithLifecycle()
    if (showWarning)
        PermissionWarning { edit() }

}

@Composable
private fun PermissionWarning(onClick: () -> Unit) {

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Warning: Missing Folder Permissions" }
            .focusable()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Lock,
                null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .semantics { hideFromAccessibility() }
            )
            Text(
                "Missing Folder Permissions", modifier = Modifier
                    .weight(1f)
                    .focusable()
                    .semantics { hideFromAccessibility() })
            TextButton(
                onClick = { onClick() },
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                ), // Reset padding
                modifier = Modifier.semantics {
                    onClick(
                        label = "Update Folder Permissions",
                        null
                    )
                },
            ) {
                Text(text = "Update")
            }
        }
    }
}
