package app.zimly.backup.ui.screens.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.db.ZimlyDatabase
import app.zimly.backup.data.db.sync.SyncProfile
import app.zimly.backup.data.db.sync.SyncDao
import app.zimly.backup.data.db.sync.SyncPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class StartViewModel(private val dataStore: SyncDao) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                val db = ZimlyDatabase.getInstance(application.applicationContext)
                val syncDao = db.syncDao()
                StartViewModel(syncDao)
            }
        }
    }

    private val selected = mutableListOf<Int>()
    // This needs to be a flow of List, not a flow of selected item. Think about it.
    private val _selectedState = MutableStateFlow(emptyList<Int>())

    private val _syncProfiles = dataStore.getAll()
    // Combines syncProfiles from DB with the selected state into a specialized
    // list of SyncProfileState data objects
    val syncProfilesState = combine(_syncProfiles, _selectedState)
    { syncProfiles, sel -> syncProfiles.map { SyncProfileState(it.uid!!, it.name, it.url, it.contentType, it.direction, sel.contains(it.uid)) } }

    // Notification displayed in the SnackBar upon successful copy/delete operations.
    private val _notification = MutableStateFlow<String?>(null)
    val notificationState = _notification.asStateFlow()

    fun select(syncProfileId: Int) {
        if (selected.contains(syncProfileId)) {
            selected.remove(syncProfileId)
        } else {
            selected.add(syncProfileId)
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
        // TODO: #map and #insert(profiles, paths)?
        selected.forEach {
            val selected = dataStore.loadById(it)
            val profileCopy = SyncProfile(null, "${selected.profile.name} (Copy)", selected.profile.url, selected.profile.key, selected.profile.secret, selected.profile.bucket, selected.profile.region, selected.profile.virtualHostedStyle, selected.profile.contentType, selected.profile.direction)

            val newId = dataStore.insert(profileCopy)
            val pathCopy = SyncPath(null, newId.toInt(), selected.paths[0].uri)
            dataStore.insert(pathCopy)
        }
        _notification.value = "Successfully copied ${selected.size} items"
        resetSelect()
    }

    fun clearMessage() {
        _notification.value = null
    }
}
