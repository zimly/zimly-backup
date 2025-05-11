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
import app.zimly.backup.ui.screens.editor.DocumentsFolderSelector
import app.zimly.backup.ui.screens.editor.EditorViewModel
import app.zimly.backup.ui.screens.editor.WizardStep
import app.zimly.backup.ui.screens.editor.form.field.UriField
import kotlinx.coroutines.flow.Flow

class DownloadTargetViewModel(private val store: ValueStore<Pair<ContentType, String>>) : ViewModel() {

    val folderField = UriField("Select a folder for your data")

    init {
        store.load()?.let { folderField.update(it.second.toUri()) }
    }

    fun persist() {
        val value = folderField.state.value.value.toString()
        store.persist(Pair(ContentType.FOLDER, value))
    }

    fun isValid(): Flow<Boolean> {
        return folderField.valid()
    }

    companion object {
        val TAG: String? = EditorViewModel::class.simpleName

        // Optional remote ID
        val VALUE_STORE_KEY = object : CreationExtras.Key<ValueStore<Pair<ContentType, String>>> {}

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
    store: ValueStore<Pair<ContentType, String>>,
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
        DocumentsFolderSelector(viewModel.folderField)
    }

}