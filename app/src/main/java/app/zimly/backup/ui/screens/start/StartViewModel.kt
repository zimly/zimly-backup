package app.zimly.backup.ui.screens.start

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.db.ZimlyDatabase
import app.zimly.backup.data.db.remote.Remote
import app.zimly.backup.data.db.remote.RemoteDao
import app.zimly.backup.data.media.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class StartViewModel(private val dataStore: RemoteDao) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                val db = ZimlyDatabase.getInstance(application.applicationContext)
                val remoteDao = db.remoteDao()
                StartViewModel(remoteDao)
            }
        }
    }

    private val selected = mutableListOf<Int>()
    // This needs to be a flow of List, not a flow of selected item. Think about it.
    private val _selectedState = MutableStateFlow(emptyList<Int>())

    private val _remotes = dataStore.getAll()
    // Combines remotes from DB with the selected state into a specialized
    // list of RemoteView data objects
    val remotesState = combine(_remotes, _selectedState)
    { remotes, sel -> remotes.map { RemoteView(it.uid!!, it.name, it.url, it.contentType, sel.contains(it.uid)) } }

    // Notification displayed in the SnackBar upon successful copy/delete operations.
    private val _notification = MutableStateFlow<String?>(null)
    val notificationState = _notification.asStateFlow()

    fun select(remoteId: Int) {
        if (selected.contains(remoteId)) {
            selected.remove(remoteId)
        } else {
            selected.add(remoteId)
        }
        // Trigger state update
        _selectedState.value = selected.toList()
    }

    fun numSelected(): Flow<Int> {
        return _selectedState.map { it.size }
    }

    fun resetSelect() {
        selected.removeAll { true }
        // Trigger state update
        _selectedState.value = selected.toList()
    }

    suspend fun delete() {
        selected.forEach { dataStore.deleteById(it) }
        _notification.value = "Successfully deleted ${selected.size} items"
        resetSelect()
    }

    suspend fun copy() {
        selected.forEach {
            val sel = dataStore.loadById(it)
            val copy = Remote(null, "${sel.name} (Copy)", sel.url, sel.key, sel.secret, sel.bucket, sel.region, ContentType.MEDIA, sel.contentUri)

            dataStore.insert(copy)
        }
        _notification.value = "Successfully copied ${selected.size} items"
        resetSelect()
    }

    fun clearMessage() {
        _notification.value = null
    }
}
