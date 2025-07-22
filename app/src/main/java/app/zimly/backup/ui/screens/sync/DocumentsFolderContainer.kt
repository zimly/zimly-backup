package app.zimly.backup.ui.screens.sync

import android.net.Uri
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.media.LocalContentResolver
import app.zimly.backup.data.media.LocalDocumentsResolver
import app.zimly.backup.ui.screens.editor.form.field.UriField
import app.zimly.backup.ui.screens.sync.DocumentsFolderViewModel.Companion.factory
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import androidx.core.net.toUri

@Composable
fun DocumentsFolderContainer(
    folderPath: String,
    viewModel: DocumentsFolderViewModel = viewModel(factory = factory(folderPath)),
) {
    val folder by viewModel.folderState.collectAsStateWithLifecycle(DocumentsFolderState())

    DocumentsFolder(folder)
}

@Composable
private fun DocumentsFolder(documentsFolderState: DocumentsFolderState) {

    val cardDescription = "Folder on Device"
    val displayName = UriField.displayName(documentsFolderState.folder)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = cardDescription
            }
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Outlined.Folder,
                "Folder",
                modifier = Modifier
                    .semantics { hideFromAccessibility() }
                    .padding(top = 8.dp, end = 8.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Folder")
                Text(displayName)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Documents")
                Text(text = "${documentsFolderState.documents}")
            }
        }
    }
}

data class DocumentsFolderState(
    var folder: Uri = Uri.EMPTY,
    var documents: Int = 0,
)

class DocumentsFolderViewModel(
    localContentResolver: LocalContentResolver,
    folderPath: Uri
) : ViewModel() {

    val folderState = snapshotFlow { folderPath }.map {
        val documentsCount = localContentResolver.listObjects().size
        return@map DocumentsFolderState(it, documentsCount)
    }.flowOn(Dispatchers.IO)

    companion object {

        val factory: (folderPath: String) -> ViewModelProvider.Factory = { folderPath ->
            viewModelFactory {
                initializer {
                    val application = checkNotNull(this[APPLICATION_KEY])

                    val folderUri = folderPath.toUri()
                    val contentResolver = LocalDocumentsResolver(application.applicationContext, folderUri)

                    DocumentsFolderViewModel(contentResolver, folderUri)
                }
            }
        }
    }
}