package io.zeitmaschine.zimzync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import io.zeitmaschine.zimzync.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

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

        var prog = 0
        var total = 0
        fun progress() {
            prog++
            setProgressAsync(Data.Builder()
                .putInt("progress", prog)
                .putInt("total", total)
                .build())
            Log.i(TAG, " Progress: $prog / $total")

        }

        syncService.sync(diff, ::progress)
        return Result.success()
    }
}

object SyncConstants {
    const val S3_URL = "url"
    const val S3_KEY = "key"
    const val S3_SECRET = "secret"
    const val S3_BUCKET = "bucket"
}