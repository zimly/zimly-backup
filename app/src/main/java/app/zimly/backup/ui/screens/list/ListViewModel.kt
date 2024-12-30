package app.zimly.backup.ui.screens.list

import androidx.lifecycle.ViewModel
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.data.remote.Remote
import app.zimly.backup.data.remote.RemoteDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class ListViewModel(private val dataStore: RemoteDao) : ViewModel() {

    private val selected = mutableListOf<Int>()
    // This needs to be a flow of List, not a flow of selected item. Think about it.
    private val _selectedState = MutableStateFlow(emptyList<Int>())

    private val _remotes = dataStore.getAll()
    // Combines remotes from DB with the selected state into a specialized
    // list of RemoteView data objects
    val remotesState = combine(_remotes, _selectedState)
    { remotes, sel -> remotes.map { RemoteView(it.uid!!, it.name, it.url, sel.contains(it.uid)) } }

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

    fun numSelected(): Int {
        return selected.size
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
            val copy = Remote(null, "${sel.name} (Copy)", sel.url, sel.key, sel.secret, sel.bucket, SourceType.MEDIA, sel.sourceUri)

            dataStore.insert(copy)
        }
        _notification.value = "Successfully copied ${selected.size} items"
        resetSelect()
    }

    fun clearMessage() {
        _notification.value = null
    }
}
