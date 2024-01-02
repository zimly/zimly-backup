package io.zeitmaschine.zimzync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters

const val SYNC_COUNT = "synced"
const val SYNC_BYTES = "size"
const val SYNC_ERROR = "error"
private const val DIFF_TOTAL = "total"

class SyncWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val syncService: SyncService
) : CoroutineWorker(context, workerParameters) {

    companion object {
        val TAG: String? = SyncWorker::class.simpleName
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Launching sync...")

        val contentBuckets =
            inputData.getStringArray(SyncConstants.DEVICE_FOLDER)?.toSet() ?: emptySet()
        val diff = syncService.diffA(contentBuckets)

        val total = diff.diff.size
        var synced = 0
        var syncedSize: Long = 0
        fun progress(size: Long) {
            ++synced
            syncedSize += size
            setProgressAsync(
                Data.Builder()
                    .putInt(SYNC_COUNT, synced)
                    .putLong(SYNC_BYTES, syncedSize)
                    .putInt(DIFF_TOTAL, total)
                    .build()
            )
            Log.i(TAG, " Progress: $synced / $total Traffic: $syncedSize")
        }

        val result = try {
            syncService.sync(diff, ::progress)
            Result.success(
                Data.Builder()
                    .putInt(SYNC_COUNT, synced)
                    .putLong(SYNC_BYTES, syncedSize)
                    .putInt(DIFF_TOTAL, total)
                    .build()
            )
        } catch (e: Exception) {
            Result.failure(
                Data.Builder()
                    .putInt(SYNC_COUNT, synced)
                    .putLong(SYNC_BYTES, syncedSize)
                    .putInt(DIFF_TOTAL, total)
                    .putString(SYNC_ERROR, e.message)
                    .build()
            )
        }
        return result
    }
}

object SyncConstants {
    const val S3_URL = "url"
    const val S3_KEY = "key"
    const val S3_SECRET = "secret"
    const val S3_BUCKET = "bucket"
    const val DEVICE_FOLDER = "folder"
}