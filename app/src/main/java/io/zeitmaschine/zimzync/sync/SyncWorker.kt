package io.zeitmaschine.zimzync.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

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
            syncService.diff(contentBuckets)
        } catch (e: Exception) {
            return Result.failure(
                Data.Builder()
                    .putString(SyncOutputs.ERROR, e.message)
                    .build()
            )
        }

        val diffCount = diff.diff.size
        val diffBytes = diff.size

        // Set initial diff numbers
        setProgress(
            Data.Builder()
                .putInt(SyncOutputs.DIFF_COUNT, diffCount)
                .putLong(SyncOutputs.DIFF_BYTES, diffBytes)
                .build()
        )

        return syncService.sync(diff, 500)
            .onEach {
                setProgressAsync(
                    Data.Builder()
                        .putInt(SyncOutputs.PROGRESS_COUNT, it.uploadedFiles)
                        .putLong(SyncOutputs.PROGRESS_BYTES, it.readBytes)
                        .putFloat(SyncOutputs.PROGRESS_PERCENTAGE, it.percentage)
                        .putInt(SyncOutputs.DIFF_COUNT, diffCount)
                        .putLong(SyncOutputs.DIFF_BYTES, diffBytes)
                        .build()
                )
            }
            .map {
                Result.success(
                    Data.Builder()
                        .putInt(SyncOutputs.PROGRESS_COUNT, it.uploadedFiles)
                        .putLong(SyncOutputs.PROGRESS_BYTES, it.readBytes)
                        .putFloat(SyncOutputs.PROGRESS_PERCENTAGE, it.percentage)
                        .putInt(SyncOutputs.DIFF_COUNT, diffCount)
                        .putLong(SyncOutputs.DIFF_BYTES, diffBytes)
                        .build()
                )
            }
            .catch {
                Log.e(TAG, "Failed to sync diff.", it)
                emit(
                    Result.failure(
                        Data.Builder()
                            .putString(SyncOutputs.ERROR, it.message)
                            .build()
                    )
                )
            }
            .last()
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