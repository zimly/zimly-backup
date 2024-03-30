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

        val diffCount = diff.diff.size
        val diffBytes = diff.size
        var progressCount = 0
        var progressBytes: Long = 0
        var progressPercentage = 0F
        fun progress(size: Long) {
            ++progressCount
            progressBytes += size

            if (progressBytes > 0) {
                progressPercentage = progressBytes.toFloat() / diffBytes
            }
            setProgressAsync(
                Data.Builder()
                    .putInt(SyncOutputs.PROGRESS_COUNT, progressCount)
                    .putLong(SyncOutputs.PROGRESS_BYTES, progressBytes)
                    .putFloat(SyncOutputs.PROGRESS_PERCENTAGE, progressPercentage)
                    .putInt(SyncOutputs.DIFF_COUNT, diffCount)
                    .putLong(SyncOutputs.DIFF_BYTES, diffBytes)
                    .build()
            )
            Log.i(TAG, " Progress: $progressCount / $diffCount Traffic: $progressBytes")
        }

        val result = try {
            syncService.sync(diff, ::progress)
            Result.success(
                Data.Builder()
                    .putInt(SyncOutputs.PROGRESS_COUNT, progressCount)
                    .putLong(SyncOutputs.PROGRESS_BYTES, progressBytes)
                    .putFloat(SyncOutputs.PROGRESS_PERCENTAGE, progressPercentage)
                    .putInt(SyncOutputs.DIFF_COUNT, diffCount)
                    .putLong(SyncOutputs.DIFF_BYTES, diffBytes)
                    .build()
            )
        } catch (e: Exception) {
            Result.failure(
                Data.Builder()
                    .putInt(SyncOutputs.PROGRESS_COUNT, progressCount)
                    .putLong(SyncOutputs.PROGRESS_BYTES, progressBytes)
                    .putFloat(SyncOutputs.PROGRESS_PERCENTAGE, progressPercentage)
                    .putInt(SyncOutputs.DIFF_COUNT, diffCount)
                    .putLong(SyncOutputs.DIFF_BYTES, diffBytes)
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
    const val PROGRESS_COUNT = "progress_count"
    const val PROGRESS_BYTES = "progress_bytes"
    const val PROGRESS_PERCENTAGE = "progress_percentage"
    const val DIFF_COUNT = "diff_count"
    const val DIFF_BYTES = "diff_bytes"
    const val ERROR = "error"
}