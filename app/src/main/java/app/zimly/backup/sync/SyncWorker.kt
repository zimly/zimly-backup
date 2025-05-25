package app.zimly.backup.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import app.zimly.backup.data.db.ZimlyDatabase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.IOException

class SyncWorker(
    private val context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {

        val syncService = try {
            initSyncService(context, inputData)
        } catch (e: Exception) {
            return Result.failure(
                Data.Builder()
                    .putString(SyncOutputs.ERROR, "Failed to initiate sync: ${e.message}")
                    .build()
            )
        }

        Log.i(TAG, "Launching sync...")

        return syncService.synchronize()
            .onEach {
                setProgressAsync(
                    Data.Builder()
                        .putInt(SyncOutputs.PROGRESS_COUNT, it.transferredFiles)
                        .putLong(SyncOutputs.PROGRESS_BYTES, it.transferredBytes)
                        .putIfNotNull(SyncOutputs.PROGRESS_BYTES_PER_SEC, it.bytesPerSecond)
                        .putFloat(SyncOutputs.PROGRESS_PERCENTAGE, it.percentage)
                        .putInt(SyncOutputs.DIFF_COUNT, it.totalFiles)
                        .putLong(SyncOutputs.DIFF_BYTES, it.totalBytes)
                        .build()
                )
            }
            .map {
                Result.success(
                    Data.Builder()
                        .putInt(SyncOutputs.PROGRESS_COUNT, it.transferredFiles)
                        .putLong(SyncOutputs.PROGRESS_BYTES, it.transferredBytes)
                        .putIfNotNull(SyncOutputs.PROGRESS_BYTES_PER_SEC, it.bytesPerSecond)
                        .putFloat(SyncOutputs.PROGRESS_PERCENTAGE, it.percentage)
                        .putInt(SyncOutputs.DIFF_COUNT, it.totalFiles)
                        .putLong(SyncOutputs.DIFF_BYTES, it.totalBytes)
                        .build()
                )
            }
            .catch {
                Log.e(TAG, "Failed to sync diff.", it)

                emit(onError(it))
            }
            .last()
    }

    /**
     * Fail or retry depending on the thrown error.
     */
    private fun onError(it: Throwable): Result {
        val result = when (it) {
            is IOException -> Result.retry() // Network errors → retry
            else -> Result.failure(
                Data.Builder()
                    .putString(SyncOutputs.ERROR, it.message)
                    .build()
            )
        }
        return result
    }

    companion object {
        val TAG: String? = SyncWorker::class.simpleName

        // TODO: provider passed through constructor?
        suspend fun initSyncService(context: Context, inputData: Data): SyncService {

            val db = ZimlyDatabase.getInstance(context.applicationContext)
            val dao = db.remoteDao()

            val remoteId = inputData.getInt(SyncInputs.REMOTE_CONFIGURATION_ID, -1)
            if (remoteId == -1) {
                throw IllegalArgumentException("Remote configuration cannot be loaded, no ID passed.")
            }
            val remote = dao.loadById(remoteId)

            return SyncService.get(context, remote)
        }
    }
}

object SyncInputs {
    const val REMOTE_CONFIGURATION_ID = "remote_id"
}

object SyncOutputs {
    const val PROGRESS_COUNT = "progress_count"
    const val PROGRESS_BYTES = "progress_bytes"
    const val PROGRESS_BYTES_PER_SEC = "progress_bytes_per_sec"
    const val PROGRESS_PERCENTAGE = "progress_percentage"
    const val DIFF_COUNT = "diff_count"
    const val DIFF_BYTES = "diff_bytes"
    const val ERROR = "error"
}
