package io.zeitmaschine.zimzync.ui.screens.list

import androidx.lifecycle.ViewModel
import io.zeitmaschine.zimzync.data.remote.Remote
import io.zeitmaschine.zimzync.data.remote.RemoteDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class ListViewModel(private val dataStore: RemoteDao) : ViewModel() {

    private val selected = mutableListOf<Int>()
    private val _selectedState = MutableStateFlow(emptyList<Int>())

    private val _remotes = dataStore.getAll()

    val remotesState = combine(_remotes, _selectedState)
    { remotes, sel -> remotes.map { RemoteView(it.uid!!, it.name, it.url, sel.contains(it.uid)) } }


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
        resetSelect()
    }

    suspend fun copy() {
        selected.forEach {
            val sel = dataStore.loadById(it)
            val copy = Remote(null, "${sel.name} (Copy)", sel.url, sel.key, sel.secret, sel.bucket, sel.folder)

            dataStore.insert(copy)
        }
        resetSelect()
    }
}
