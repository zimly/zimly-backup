package io.zeitmaschine.zimzync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
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

    override suspend fun doWork(): Result {
        val diff = syncService.diffA()
        syncService.sync(diff)
        return Result.success()
    }
}

object SyncConstants {
    const val S3_URL = "url"
    const val S3_KEY = "key"
    const val S3_SECRET = "secret"
    const val S3_BUCKET = "bucket"
}