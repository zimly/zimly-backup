package io.zeitmaschine.zimzync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters

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
            inputData.getStringArray(SyncInputs.DEVICE_FOLDER)?.toSet() ?: emptySet()

        val diff = try {
            syncService.diffA(contentBuckets)
        } catch (e: Exception) {
            return Result.failure(
                Data.Builder()
                    .putString(SyncOutputs.ERROR, e.message)
                    .build()
            )
        }

        val total = diff.diff.size
        var synced = 0
        var syncedSize: Long = 0
        fun progress(size: Long) {
            ++synced
            syncedSize += size
            setProgressAsync(
                Data.Builder()
                    .putInt(SyncOutputs.SYNCED_FILES, synced)
                    .putLong(SyncOutputs.SYNCED_BYTES, syncedSize)
                    .putInt(SyncOutputs.DIFF_FILES, total)
                    .build()
            )
            Log.i(TAG, " Progress: $synced / $total Traffic: $syncedSize")
        }

        val result = try {
            syncService.sync(diff, ::progress)
            Result.success(
                Data.Builder()
                    .putInt(SyncOutputs.SYNCED_FILES, synced)
                    .putLong(SyncOutputs.SYNCED_BYTES, syncedSize)
                    .putInt(SyncOutputs.DIFF_FILES, total)
                    .build()
            )
        } catch (e: Exception) {
            Result.failure(
                Data.Builder()
                    .putInt(SyncOutputs.SYNCED_FILES, synced)
                    .putLong(SyncOutputs.SYNCED_BYTES, syncedSize)
                    .putInt(SyncOutputs.DIFF_FILES, total)
                    .putString(SyncOutputs.ERROR, e.message)
                    .build()
            )
        }
        return result
    }
}

object SyncInputs {
    const val S3_URL = "url"
    const val S3_KEY = "key"
    const val S3_SECRET = "secret"
    const val S3_BUCKET = "bucket"
    const val DEVICE_FOLDER = "folder"
}

object SyncOutputs {
    const val SYNCED_FILES = "synced"
    const val SYNCED_BYTES = "size"
    const val DIFF_FILES = "total"
    const val ERROR = "error"
}