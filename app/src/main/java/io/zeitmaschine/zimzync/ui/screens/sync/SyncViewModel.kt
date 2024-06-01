package io.zeitmaschine.zimzync.ui.screens.sync

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.workDataOf
import io.zeitmaschine.zimzync.data.media.MediaRepository
import io.zeitmaschine.zimzync.data.remote.RemoteDao
import io.zeitmaschine.zimzync.data.s3.MinioRepository
import io.zeitmaschine.zimzync.sync.Diff
import io.zeitmaschine.zimzync.sync.SyncInputs
import io.zeitmaschine.zimzync.sync.SyncOutputs
import io.zeitmaschine.zimzync.sync.SyncServiceImpl
import io.zeitmaschine.zimzync.sync.SyncWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModel(
    private val dao: RemoteDao,
    private val remoteId: Int,
    private val workManager: WorkManager,
    private val mediaRepo: MediaRepository
): ViewModel() {

    // Todo: https://luisramos.dev/testing-your-android-viewmodel
    companion object {
        val TAG: String? = SyncViewModel::class.simpleName
    }

    // This identifier is used to identify already running sync instances and prevent simultaneous
    // sync-executions.
    private var uniqueWorkIdentifier = "sync_${remoteId}"

    var remoteState: Flow<RemoteState> = snapshotFlow { remoteId }
        .map { dao.loadById(it) }
        .map {
            RemoteState(
                name = it.name,
                url = it.url,
                bucket = it.bucket,
                folder = it.folder
            )
        }

    var folderState = remoteState.map {
        val photoCount = mediaRepo.getPhotos(setOf(it.folder)).size
        val videoCount = mediaRepo.getVideos(setOf(it.folder)).size
        return@map FolderState(it.folder, photoCount, videoCount)
    }

    // Flow created when starting the sync
    // TODO change to MutableSharedFlow as well!?
    private val _startedSyncId: MutableStateFlow<UUID?> = MutableStateFlow(null)

    // Flow for already running sync jobs
    private val _runningSyncId: Flow<UUID?> = loadSyncState()

    private val _observedProgress = merge(_runningSyncId, _startedSyncId)
        .onEach { Log.i(TAG, "syncId: $it") }
        .filterNotNull()
        .flatMapLatest { observeSyncProgress(it) }

    private val _diff: MutableStateFlow<Diff?> = MutableStateFlow(null)
    private val _diffProgress: Flow<Progress> = _diff.filterNotNull().map {
        Progress(
            diffBytes = it.size,
            diffCount = it.diff.size
        )
    }

    // merge the flows from observed progress and one time diff
    var progressState: StateFlow<Progress> = merge(_observedProgress, _diffProgress)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Progress(),
        )

    // mutable error flow, for manually triggered errors
    private val _error: MutableSharedFlow<String?> = MutableSharedFlow()

    // Merge errors from progress and manually triggered errors into one observable for the UI
    val error: StateFlow<String?> = merge(_error, progressState.mapNotNull { it.error })
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    suspend fun createDiff() {

        try {
            val remote = dao.loadById(remoteId)
            val s3Repo = MinioRepository(remote.url, remote.key, remote.secret, remote.bucket)
            val syncService = SyncServiceImpl(s3Repo, mediaRepo)

            val diff = syncService.diff(setOf(remote.folder))
            // Display result of the minio request to the user
            _diff.update { diff }
        } catch (e: Exception) {
            _error.emit( e.message ?: "Unknown error." )
        }
    }

    // TODO https://code.luasoftware.com/tutorials/android/jetpack-compose-load-data
    private fun loadSyncState(): Flow<UUID> {
        val query = WorkQuery.Builder.fromUniqueWorkNames(listOf(uniqueWorkIdentifier))
            .addStates(listOf(WorkInfo.State.RUNNING))
            .build()
        return workManager.getWorkInfosFlow(query)
            .onEach { require(it.size < 2)  { "More than one unique sync job in progress. This should not happen." } }
            .flatMapLatest { it.asFlow() }
            .map { it.id }
    }

    private fun observeSyncProgress(id: UUID): Flow<Progress> {
        return workManager.getWorkInfoByIdFlow(id)
            .filterNotNull()
            // filter out unhandled states that result in a "nulled" out progress object.
            .filter { it.state in arrayOf(
                WorkInfo.State.SUCCEEDED,
                WorkInfo.State.RUNNING,
                WorkInfo.State.FAILED
            ) }
            .map { workInfo ->

            val progressState = Progress()

            when (workInfo.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress
                    progressState.progressCount = progress.getInt(SyncOutputs.PROGRESS_COUNT, 0)
                    progressState.progressBytes = progress.getLong(SyncOutputs.PROGRESS_BYTES, 0)
                    progressState.percentage = progress.getFloat(SyncOutputs.PROGRESS_PERCENTAGE, 0F)
                    progressState.diffCount = progress.getInt(SyncOutputs.DIFF_COUNT, 0)
                    progressState.diffBytes = progress.getLong(SyncOutputs.DIFF_BYTES, 0)
                    progressState.inProgress = true
                }

                WorkInfo.State.SUCCEEDED -> {
                    val output = workInfo.outputData
                    progressState.progressCount = output.getInt(SyncOutputs.PROGRESS_COUNT, 0)
                    progressState.progressBytes = output.getLong(SyncOutputs.PROGRESS_BYTES, 0)
                    progressState.percentage = output.getFloat(SyncOutputs.PROGRESS_PERCENTAGE, 0F)
                    progressState.diffCount = output.getInt(SyncOutputs.DIFF_COUNT, 0)
                    progressState.diffBytes = output.getLong(SyncOutputs.DIFF_BYTES, 0)
                    progressState.inProgress = false
                }

                WorkInfo.State.FAILED -> {
                    val output = workInfo.outputData
                    progressState.progressCount = output.getInt(SyncOutputs.PROGRESS_COUNT, 0)
                    progressState.progressBytes = output.getLong(SyncOutputs.PROGRESS_BYTES, 0)
                    progressState.percentage = output.getFloat(SyncOutputs.PROGRESS_PERCENTAGE, 0F)
                    progressState.diffCount = output.getInt(SyncOutputs.DIFF_COUNT, 0)
                    progressState.diffBytes = output.getLong(SyncOutputs.DIFF_BYTES, 0)
                    progressState.error = output.getString(SyncOutputs.ERROR) ?: "Unknown error."
                    progressState.inProgress = false
                }

                else -> {
                    Log.e(TAG, "State '${workInfo.state}' should not be observed.")
                }
            }
            progressState
        }
    }

    // TODO check error handling!
    suspend fun sync(): UUID {
        val remote = dao.loadById(remoteId)
        val data = workDataOf(
            SyncInputs.S3_URL to remote.url,
            SyncInputs.S3_KEY to remote.key,
            SyncInputs.S3_SECRET to remote.secret,
            SyncInputs.S3_BUCKET to remote.bucket,
            SyncInputs.DEVICE_FOLDER to arrayOf(remote.folder)
        )
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(uniqueWorkIdentifier, ExistingWorkPolicy.KEEP, syncRequest)
        _startedSyncId.update { syncRequest.id }

        return syncRequest.id
    }

    suspend fun clearError() {
        _error.emit( null)
    }

    data class RemoteState(
        var name: String = "",
        var url: String = "",
        var bucket: String = "",
        var folder: String = "",
    )

    data class FolderState(
        var folder: String = "",
        var photos: Int = 0,
        var videos: Int = 0,
    )

    data class Progress(
        var percentage: Float = 0.0f,
        var progressCount: Int = 0,
        var progressBytes: Long = 0,
        var diffCount: Int = 0,
        var diffBytes: Long = 0,
        var inProgress: Boolean = false,
        var error: String? = null,
    )
}