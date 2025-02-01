package app.zimly.backup.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import app.zimly.backup.data.media.LocalDocumentsResolver
import app.zimly.backup.data.media.LocalMediaResolver
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.data.s3.MinioRepository
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

        Log.i(TAG, "Launching sync...")

        val diff = try {
            syncService.diff()
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

        return syncService.sync(diff)
            .onEach {
                setProgressAsync(
                    Data.Builder()
                        .putInt(SyncOutputs.PROGRESS_COUNT, it.uploadedFiles)
                        .putLong(SyncOutputs.PROGRESS_BYTES, it.readBytes)
                        .putIfNotNull(SyncOutputs.PROGRESS_BYTES_PER_SEC, it.bytesPerSecond)
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
                        .putIfNotNull(SyncOutputs.PROGRESS_BYTES_PER_SEC, it.bytesPerSecond)
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
            val url = requireNotNull(inputData.getString(SyncInputs.S3_URL)) { "URL cannot be empty" }
            val key = requireNotNull(inputData.getString(SyncInputs.S3_KEY)) { "Bucket key cannot be empty" }
            val secret = requireNotNull(inputData.getString(SyncInputs.S3_SECRET)) { "Bucket secret cannot be empty" }
            val bucket = requireNotNull(inputData.getString(SyncInputs.S3_BUCKET)) { "Bucket cannot be empty" }

            val sourceType = inputData.getString(SyncInputs.SOURCE_TYPE).let {
                requireNotNull(it) { "Source Type cannot be empty" }
                SourceType.valueOf(it)
            }
            val sourcePath = requireNotNull(inputData.getString(SyncInputs.SOURCE_PATH)) { "Source Type cannot be empty" }

            val s3Repository = MinioRepository(url, key, secret, bucket)

            val localMediaResolver = when (sourceType) {
                SourceType.MEDIA -> LocalMediaResolver(context.contentResolver, sourcePath)
                SourceType.FOLDER -> LocalDocumentsResolver(context.contentResolver, sourcePath)
            }

            return SyncServiceImpl(s3Repository, localMediaResolver)
        }
    }
}

object SyncInputs {
    const val S3_URL = "url"
    const val S3_KEY = "key"
    const val S3_SECRET = "secret"
    const val S3_BUCKET = "bucket"
    const val SOURCE_TYPE = "source_type"
    const val SOURCE_PATH = "source_path"
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
