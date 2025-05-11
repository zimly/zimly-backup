package app.zimly.backup.ui.screens.editor.steps

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.media.ContentType
import app.zimly.backup.ui.screens.editor.BackupSourceConfiguration
import app.zimly.backup.ui.screens.editor.EditorViewModel
import app.zimly.backup.ui.screens.editor.WizardStep
import app.zimly.backup.ui.screens.editor.form.field.BackupSourceField
import kotlinx.coroutines.flow.Flow

class UploadSourceViewModel(private val store: ValueStore<Pair<ContentType, String>>) : ViewModel() {

    val backupSource = BackupSourceField()

    init {
        val value = store.load()
        if (value != null) {
            backupSource.update(value.first)
            when (value.first) {
                ContentType.MEDIA -> backupSource.mediaField.update(value.second)
                ContentType.FOLDER -> backupSource.folderField.update(value.second.toUri())
            }
        }
    }

    fun persist() {
        val sourceType = backupSource.state.value.type
        val sourceUri = when (sourceType) {
            ContentType.MEDIA -> backupSource.mediaField.state.value.value
            ContentType.FOLDER -> backupSource.folderField.state.value.value.toString()
        }
        store.persist(Pair(sourceType, sourceUri))
    }

    fun isValid(): Flow<Boolean> {
        return backupSource.valid()
    }

    companion object {
        val TAG: String? = EditorViewModel::class.simpleName

        // Optional remote ID
        val VALUE_STORE_KEY = object : CreationExtras.Key<ValueStore<Pair<ContentType, String>>> {}

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
    contentStore: ValueStore<Pair<ContentType, String>>,
    nextStep: () -> Unit,
    previousStep: () -> Unit,
    viewModel: UploadSourceViewModel = viewModel(
        factory = UploadSourceViewModel.Factory,
        extras = MutableCreationExtras().apply {
            set(UploadSourceViewModel.VALUE_STORE_KEY, contentStore)
        })
) {

    val valid by viewModel.isValid().collectAsStateWithLifecycle(false)

    WizardStep(
        title = "Select Upload Source",
        navigation = {
            TextButton(onClick = { previousStep() }) {
                Text("Back")
            }
            TextButton(
                enabled = valid,
                onClick = {
                    viewModel.persist()
                    nextStep()
                },
            ) {
                Text("Continue")
            }
        }
    ) {
        BackupSourceConfiguration(viewModel.backupSource)
    }

}