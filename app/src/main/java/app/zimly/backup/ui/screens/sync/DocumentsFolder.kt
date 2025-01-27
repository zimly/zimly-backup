package app.zimly.backup.ui.screens.sync

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.media.LocalDocumentsResolver
import app.zimly.backup.ui.screens.editor.field.UriField
import app.zimly.backup.ui.screens.sync.SyncViewModel.SyncConfigurationState
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
fun DocumentsFolderCompose(
    folderPath: String,
    syncConfigurationFlow: Flow<SyncConfigurationState>,
    application: Application,
    viewModel: DocumentsFolderViewModel = viewModel(factory = viewModelFactory {
        initializer {
            val localDocumentsResolver = LocalDocumentsResolver(application.contentResolver, folderPath)
            DocumentsFolderViewModel(localDocumentsResolver, syncConfigurationFlow)
        }
    }),
) {

    val folder by viewModel.folderState.collectAsStateWithLifecycle(DocumentsFolderState())

    DocumentsFolder(folder)
}

@Composable
private fun DocumentsFolder(documentsFolderState: DocumentsFolderState) {

    val displayName = UriField.displayName(documentsFolderState.folder)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Outlined.Folder,
                "Media",
                modifier = Modifier.padding(top = 8.dp, end = 8.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Folder")
                displayName?.let { Text(it) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Documents")
                Text(text = "${documentsFolderState.documents}")
            }
        }
    }
}

data class DocumentsFolderState(
    var folder: String = "",
    var documents: Int = 0,
)

class DocumentsFolderViewModel(
    localContentResolver: LocalDocumentsResolver,
    syncConfigurationFlow: Flow<SyncConfigurationState>
) : ViewModel() {

    val folderState = syncConfigurationFlow.map {
        val documentsCount = localContentResolver.listObjects().size
        return@map DocumentsFolderState(it.sourceUri, documentsCount)
    }
}