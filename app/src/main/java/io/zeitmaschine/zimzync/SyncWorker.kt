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

        val diff = syncService.diffA()

        var synced = 0
        var syncedSize: Long = 0
        var total = diff.diff.size
        fun progress(size: Long) {
            ++synced
            syncedSize += size
            setProgressAsync(Data.Builder()
                .putInt("synced", synced)
                .putLong("size", syncedSize)
                .putInt("total", total)
                .build())
            Log.i(TAG, " Progress: $synced / $total Traffic: $syncedSize")

        }

        syncService.sync(diff, ::progress)
        return Result.success(Data.Builder()
            .putInt("synced", synced)
            .putLong("size", syncedSize)
            .putInt("total", total)
            .build())
    }
}

object SyncConstants {
    const val S3_URL = "url"
    const val S3_KEY = "key"
    const val S3_SECRET = "secret"
    const val S3_BUCKET = "bucket"
}