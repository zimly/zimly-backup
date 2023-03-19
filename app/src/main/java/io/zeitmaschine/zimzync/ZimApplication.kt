package io.zeitmaschine.zimzync

import android.app.Application
import android.content.Context
import androidx.work.*

class ZimApplication() : Application(), Configuration.Provider {
    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(ZimWorkerFactory())
            .build()
}

/*

Create an own WorkerFactory for passing the SyncService to it.

https://medium.com/androiddevelopers/customizing-workmanager-fundamentals-fdaa17c46dd2
https://medium.com/tech-takeaways/use-the-android-workmanager-api-for-synchronization-tasks-c52029b2ce56
 */
class ZimWorkerFactory() : WorkerFactory() {

    // This only handles a single Worker, use DelegatingWorkerFactory, see link above.
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
        val url = workerParameters.inputData.getString(SyncConstants.S3_URL)!!
        val key = workerParameters.inputData.getString(SyncConstants.S3_KEY)!!
        val secret = workerParameters.inputData.getString(SyncConstants.S3_SECRET)!!
        val bucket = workerParameters.inputData.getString(SyncConstants.S3_BUCKET)!!

        val s3Repository = MinioRepository(url, key, secret, bucket)
        val mediaRepository = ResolverBasedRepository(appContext.contentResolver)
        val syncService = SyncServiceImpl(s3Repository, mediaRepository)
        return SyncWorker(appContext, workerParameters, syncService)

    }
}
