package io.zeitmaschine.zimzync.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import io.zeitmaschine.zimzync.data.media.ResolverBasedRepository
import io.zeitmaschine.zimzync.data.s3.MinioRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

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

        val contentBuckets = inputData.getStringArray(SyncInputs.DEVICE_FOLDER)?.toSet() ?: emptySet()

        Log.i(TAG, "Launching sync...")

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

    companion object {
        val TAG: String? = SyncWorker::class.simpleName

        fun initSyncService(context: Context, inputData: Data): SyncService {
            val url = inputData.getString(SyncInputs.S3_URL)
            requireNotNull(url) { "URL cannot be empty" }

            val key = inputData.getString(SyncInputs.S3_KEY)
            requireNotNull(key) { "Bucket key cannot be empty" }

            val secret = inputData.getString(SyncInputs.S3_SECRET)
            requireNotNull(secret) { "Bucket secret cannot be empty" }

            val bucket = inputData.getString(SyncInputs.S3_BUCKET)
            requireNotNull(bucket) { "Bucket secret cannot be empty" }

            val s3Repository = MinioRepository(url, key, secret, bucket)
            val mediaRepository = ResolverBasedRepository(context.contentResolver)

            return SyncServiceImpl(s3Repository, mediaRepository)
        }
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