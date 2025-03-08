package app.zimly.backup.ui.screens.sync

import android.content.ContentResolver
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.workDataOf
import app.zimly.backup.data.media.LocalContentResolver
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.data.remote.RemoteDao
import app.zimly.backup.data.s3.MinioRepository
import app.zimly.backup.sync.SyncInputs
import app.zimly.backup.sync.SyncOutputs
import app.zimly.backup.sync.SyncServiceImpl
import app.zimly.backup.sync.SyncWorker
import app.zimly.backup.sync.getNullable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Duration
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModel(
    private val dao: RemoteDao,
    private val remoteId: Int,
    private val workManager: WorkManager,
    // TODO keeping a ref to contentResolver might be a problem
    // --> instead extend AndroidViewModel
    private val contentResolver: ContentResolver,
    // TODO keeping a ref to application IS a problem
) : ViewModel() {

    // Todo: https://luisramos.dev/testing-your-android-viewmodel
    companion object {
        val TAG: String? = SyncViewModel::class.simpleName

        /**
         * [WorkInfo.State] non terminal states, that the will be treated as in-progress syncs.
         */
        val IN_PROGRESS_STATES: Set<WorkInfo.State> = setOf(
            WorkInfo.State.RUNNING,
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED)

        /**
         * Maps [WorkInfo.State]s to UI [Status].
         */
        fun mapState(state: WorkInfo.State): Status {
            return when(state) {
                WorkInfo.State.ENQUEUED -> Status.CONSTRAINTS_NOT_MET
                WorkInfo.State.RUNNING -> Status.IN_PROGRESS
                WorkInfo.State.SUCCEEDED -> Status.COMPLETED
                WorkInfo.State.FAILED -> Status.FAILED
                WorkInfo.State.BLOCKED -> Status.WAITING
                WorkInfo.State.CANCELLED -> Status.CANCELLED
            }
        }
    }

    // This identifier is used to identify already running sync instances and prevent simultaneous
    // sync-executions.
    private var uniqueWorkIdentifier = "sync_${remoteId}"

    var syncConfigurationState: Flow<SyncConfigurationState> = snapshotFlow { remoteId }
        .map { dao.loadById(it) }
        .map {
            SyncConfigurationState(
                name = it.name,
                url = it.url,
                bucket = it.bucket,
                sourceType = it.sourceType,
                sourceUri = it.sourceUri
            )
        }.flowOn(Dispatchers.IO)

    // Flow created when starting the sync
    // TODO change to MutableSharedFlow as well!?
    private val _startedSyncId: MutableStateFlow<UUID?> = MutableStateFlow(null)

    // Flow for already running sync jobs
    private val _runningSyncId: Flow<UUID?> = loadSyncState()

    private val _observedProgress = merge(_runningSyncId, _startedSyncId)
        .onEach { Log.i(TAG, "syncId: $it") }
        .filterNotNull()
        .flatMapLatest { observeSyncProgress(it) }

    private val _progress: MutableStateFlow<Progress> = MutableStateFlow(Progress())

    // merge the flows from observed progress and one time diff
    var progressState: StateFlow<Progress> = merge(_observedProgress, _progress)
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

    /**
     * Calculate the diff between remote bucket and the local gallery. This is a "heavy" computation.
     * Use e.g. [kotlinx.coroutines.Dispatchers.Default] to not block the Main thread. As the UI
     * [_progress] state updates use a Flow, we don't have to switch back..
     *
     * Ref:
     * https://proandroiddev.com/mutablestate-or-mutablestateflow-a-perspective-on-what-to-use-in-jetpack-compose-ccec0af7abbf
     */
    suspend fun createDiff() {

        _progress.update {
            Progress(
                status = Status.CALCULATING
            )
        }
        try {
            val remote = dao.loadById(remoteId)
            val s3Repo = MinioRepository(remote.url, remote.key, remote.secret, remote.bucket)

            val contentResolver = LocalContentResolver.get(contentResolver, remote.sourceType, remote.sourceUri)
            val syncService = SyncServiceImpl(s3Repo, contentResolver)

            val diff = syncService.diff()
            // Display result of the minio request to the user
            _progress.update {
                Progress(
                    diffBytes = diff.size,
                    diffCount = diff.diff.size,
                    status = null
                )
            }
        } catch (e: Exception) {
            // TODO progress.error instead? Basically back to one state object
            _error.emit(e.message ?: "Unknown error.")
            _progress.update {
                Progress(
                    status = null
                )
            }
        }
    }

    /**
     * Queries and returns the UUID of any [WorkInfo] in any of [IN_PROGRESS_STATES] state, identified
     * by [uniqueWorkIdentifier].
     *
     * @throws IllegalArgumentException in case more than 1 is found.
     */
    private fun loadSyncState(): Flow<UUID> {
        val query = WorkQuery.Builder.fromUniqueWorkNames(listOf(uniqueWorkIdentifier))
            .addStates(IN_PROGRESS_STATES.toList())
            .build()
        return workManager.getWorkInfosFlow(query)
            .onEach { require(it.size < 2) { "More than one unique sync job in progress. This should not happen." } }
            .flatMapLatest { it.asFlow() }
            .map { it.id }
    }

    /**
     * Creates a [Flow] of [Progress] objects for observing the progress of a given [WorkInfo].
     */
    private fun observeSyncProgress(id: UUID): Flow<Progress> {
        return workManager.getWorkInfoByIdFlow(id)
            .filterNotNull()
            // filter out unhandled states that result in a "nulled" out progress object.
            .map { workInfo ->

                val progressState = Progress()

                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress
                        progressState.progressCount = progress.getInt(SyncOutputs.PROGRESS_COUNT, 0)
                        progressState.progressBytes =
                            progress.getLong(SyncOutputs.PROGRESS_BYTES, 0)
                        progressState.progressBytesPerSec =
                            progress.getNullable(SyncOutputs.PROGRESS_BYTES_PER_SEC)
                        progressState.percentage =
                            progress.getFloat(SyncOutputs.PROGRESS_PERCENTAGE, 0F)
                        progressState.diffCount = progress.getInt(SyncOutputs.DIFF_COUNT, 0)
                        progressState.diffBytes = progress.getLong(SyncOutputs.DIFF_BYTES, 0)
                        progressState.status = mapState(workInfo.state)
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        val output = workInfo.outputData
                        progressState.progressCount = output.getInt(SyncOutputs.PROGRESS_COUNT, 0)
                        progressState.progressBytes = output.getLong(SyncOutputs.PROGRESS_BYTES, 0)
                        progressState.progressBytesPerSec =
                            output.getLong(SyncOutputs.PROGRESS_BYTES_PER_SEC, 0)
                        progressState.percentage =
                            output.getFloat(SyncOutputs.PROGRESS_PERCENTAGE, 0F)
                        progressState.diffCount = output.getInt(SyncOutputs.DIFF_COUNT, 0)
                        progressState.diffBytes = output.getLong(SyncOutputs.DIFF_BYTES, 0)
                        progressState.status = mapState(workInfo.state)
                    }

                    WorkInfo.State.ENQUEUED -> {
                        progressState.status = mapState(workInfo.state)
                    }

                    WorkInfo.State.BLOCKED -> {
                        // This should not happen in our case
                        progressState.status = mapState(workInfo.state)
                    }

                    WorkInfo.State.FAILED -> {
                        val output = workInfo.outputData
                        progressState.progressCount = output.getInt(SyncOutputs.PROGRESS_COUNT, 0)
                        progressState.progressBytes = output.getLong(SyncOutputs.PROGRESS_BYTES, 0)
                        progressState.progressBytesPerSec =
                            output.getLong(SyncOutputs.PROGRESS_BYTES_PER_SEC, 0)
                        progressState.percentage =
                            output.getFloat(SyncOutputs.PROGRESS_PERCENTAGE, 0F)
                        progressState.diffCount = output.getInt(SyncOutputs.DIFF_COUNT, 0)
                        progressState.diffBytes = output.getLong(SyncOutputs.DIFF_BYTES, 0)
                        progressState.error =
                            output.getString(SyncOutputs.ERROR) ?: "Unknown error."
                        progressState.status = mapState(workInfo.state)
                    }

                    WorkInfo.State.CANCELLED -> {
                        progressState.status = mapState(workInfo.state)
                    }
                }
                progressState
            }
    }

    suspend fun sync(): UUID {
        _progress.update {
            Progress(
                status = Status.CALCULATING
            )
        }
        val remote = dao.loadById(remoteId)
        val data = workDataOf(
            SyncInputs.S3_URL to remote.url,
            SyncInputs.S3_KEY to remote.key,
            SyncInputs.S3_SECRET to remote.secret,
            SyncInputs.S3_BUCKET to remote.bucket,
            SyncInputs.SOURCE_TYPE to remote.sourceType.name,
            SyncInputs.SOURCE_PATH to remote.sourceUri
        )
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(1)) // Exponentially retry
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(uniqueWorkIdentifier, ExistingWorkPolicy.KEEP, syncRequest)

        _startedSyncId.update { syncRequest.id }

        return syncRequest.id
    }

    fun cancelSync() {
        workManager.cancelUniqueWork(uniqueWorkIdentifier)
    }

    suspend fun clearError() {
        _error.emit(null)
    }

    data class SyncConfigurationState(
        var name: String = "",
        var url: String = "",
        var bucket: String = "",
        var sourceType: SourceType? = null,
        var sourceUri: String = "",
    )

    data class Progress(
        var percentage: Float = 0.0f,
        var progressCount: Int = 0,
        var progressBytes: Long = 0,
        var progressBytesPerSec: Long? = null,
        var diffCount: Int = -1,
        var diffBytes: Long = -1,
        var status: Status? = null,
        var error: String? = null,
    )

    enum class Status {
        CALCULATING, IN_PROGRESS, COMPLETED, CONSTRAINTS_NOT_MET, CANCELLED, WAITING, FAILED,
    }

}