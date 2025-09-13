package app.zimly.backup.ui.screens.editor.steps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.media.ContentType
import app.zimly.backup.ui.screens.editor.EditorViewModel
import app.zimly.backup.ui.screens.editor.form.field.UriPermission
import app.zimly.backup.ui.screens.editor.form.field.UriField
import app.zimly.backup.ui.screens.editor.steps.components.DocumentsFolderSelector
import app.zimly.backup.ui.screens.editor.steps.components.WizardStep
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class DownloadTargetViewModel(private val store: ValueStore<EditorViewModel.ContentState>) :
    ViewModel() {

    val folderField = UriField("Select a folder and grant permissions for your data")

    init {
        viewModelScope.launch {
            store.load()
                .filterNotNull()
                .collectLatest {
                    // Enforce the touch to propagate error related to revoked permissions
                    // Has to happen before update.
                    folderField.touch()
                    folderField.update(UriPermission(it.contentUri.toUri(), it.permissions))
                }
        }
    }

    fun persist(nextStep: () -> Unit) {
        val contentUri = folderField.state.value.value.uri.toString()
        val permission = folderField.state.value.value.permission
        store.persist(EditorViewModel.ContentState(ContentType.FOLDER, contentUri, permission)) { nextStep() }
    }

    fun isValid(): Flow<Boolean> {
        return folderField.valid()
    }

    companion object {
        val TAG: String? = DownloadTargetViewModel::class.simpleName

        val VALUE_STORE_KEY = object : CreationExtras.Key<ValueStore<EditorViewModel.ContentState>> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {

            initializer {
                val valueStore = checkNotNull(this[VALUE_STORE_KEY])
                DownloadTargetViewModel(valueStore)
            }
        }
    }


}

@Composable
fun DownloadTargetStep(
    store: ValueStore<EditorViewModel.ContentState>,
    nextStep: () -> Unit,
    previousStep: () -> Unit,
    viewModel: DownloadTargetViewModel = viewModel(
        factory = DownloadTargetViewModel.Factory,
        extras = MutableCreationExtras().apply {
            set(DownloadTargetViewModel.VALUE_STORE_KEY, store)
        })
) {

    val valid by viewModel.isValid().collectAsStateWithLifecycle(false)
    WizardStep(
        title = "Select Download Location",
        navigation = {
            TextButton(onClick = { previousStep() }) {
                Text("Back")
            }
            TextButton(
                enabled = valid,
                onClick = {
                    viewModel.persist(nextStep)
                },
            ) {
                Text("Continue")
            }
        }
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = containerBackground(),
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Outlined.Folder,
                    "Folder",
                    modifier = Modifier.padding(top = 8.dp, end = 8.dp)
                )
            }
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                DocumentsFolderSelector(viewModel.folderField)
            }
        }
    }

}