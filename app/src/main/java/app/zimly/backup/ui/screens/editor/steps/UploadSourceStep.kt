package app.zimly.backup.ui.screens.editor.steps

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import app.zimly.backup.ui.screens.editor.form.field.BackupSourceField
import app.zimly.backup.ui.screens.editor.form.field.Permissions
import app.zimly.backup.ui.screens.editor.form.field.UriPermission
import app.zimly.backup.ui.screens.editor.steps.components.BackupSourceConfiguration
import app.zimly.backup.ui.screens.editor.steps.components.WizardStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class UploadSourceViewModel(private val store: ValueStore<EditorViewModel.ContentState>) :
    ViewModel() {

    val backupSource = BackupSourceField()

    init {
        viewModelScope.launch {
            store.load()
                .filterNotNull()
                .collectLatest {
                    backupSource.update(it.contentType)
                    // Enforce the touch to propagate error related to revoked permissions,
                    // has to happen before update
                    backupSource.touch()
                    when (it.contentType) {
                        ContentType.MEDIA -> backupSource.mediaField.update(it.contentUri)
                        ContentType.FOLDER -> backupSource.folderField.update(UriPermission(it.contentUri.toUri(), it.permissions))
                    }

                }
        }
    }

    fun persist(nextStep: () -> Unit) {
        val sourceType = backupSource.state.value.type
        val sourceUri = when (sourceType) {
            ContentType.MEDIA -> backupSource.mediaField.state.value.value
            ContentType.FOLDER -> backupSource.folderField.state.value.value.uri.toString()
        }
        val permissions = when (sourceType) {
            ContentType.MEDIA -> Permissions.GRANTED
            ContentType.FOLDER -> backupSource.folderField.state.value.value.permission
        }
        store.persist(EditorViewModel.ContentState(sourceType, sourceUri, permissions)) { nextStep() }
    }

    fun isValid(): Flow<Boolean> {
        return backupSource.valid()
    }

    companion object {
        val TAG: String? = UploadSourceViewModel::class.simpleName

        val VALUE_STORE_KEY = object : CreationExtras.Key<ValueStore<EditorViewModel.ContentState>> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {

            initializer {
                val valueStore = checkNotNull(this[VALUE_STORE_KEY])
                UploadSourceViewModel(valueStore)
            }
        }
    }


}

@Composable
fun UploadSourceStep(
    store: ValueStore<EditorViewModel.ContentState>,
    nextStep: () -> Unit,
    previousStep: () -> Unit,
    viewModel: UploadSourceViewModel = viewModel(
        factory = UploadSourceViewModel.Factory,
        extras = MutableCreationExtras().apply {
            set(UploadSourceViewModel.VALUE_STORE_KEY, store)
        })
) {

    val valid by viewModel.isValid().collectAsStateWithLifecycle(false)

    WizardStep(
        title = "Select Content to Upload",
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
        BackupSourceConfiguration(viewModel.backupSource)
    }

}