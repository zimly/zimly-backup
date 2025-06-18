package app.zimly.backup.sync

import android.util.Log
import app.zimly.backup.data.media.ContentObject
import app.zimly.backup.data.media.WriteableContentResolver
import app.zimly.backup.data.s3.S3Object
import app.zimly.backup.data.s3.S3Repository
import io.minio.errors.ErrorResponseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.sample

class DownloadSyncService(
    private val s3Repository: S3Repository,
    private val localContentResolver: WriteableContentResolver,
    private val samplePeriod: Long = 500L
) : SyncService {

    companion object {
        val TAG: String? = DownloadSyncService::class.simpleName

        /**
         * Determines which remote [S3Object]s should be downloaded based on local presence and modification time.
         *
         * A remote object is selected if:
         * * It does not exist locally (by matching [S3Object.name] to [ContentObject.relPath]), or
         * * Its [S3Object.modified] is more recent than the corresponding local [ContentObject.lastModified].
         *
         * @param remotes The list of remote [S3Object]s available.
         * @param locales The list of local [ContentObject]s currently stored.
         * @return A list of remote [S3Object]s that need to be downloaded.
         */
        fun calculateDownloads(
            remotes: List<S3Object>,
            locales: List<ContentObject>
        ): List<S3Object> {

            val localsByPath = locales.associateBy { it.relPath }

            return remotes.filter { remote ->
                val local = localsByPath[remote.name]
                local == null || local.lastModified < remote.modified.toInstant().toEpochMilli()
            }
        }

    }

    override fun calculateDiff(): DownloadDiff {
        try {
            val remotes = s3Repository.listObjects()
            val objects = localContentResolver.listObjects()

            val downloads = calculateDownloads(remotes, objects)
            val size = downloads.sumOf { it.size }

            return DownloadDiff(downloads.size, size, remotes, objects, downloads)
        } catch (e: Exception) {
            var message = e.message
            if (e is ErrorResponseException) {
                val status = e.response().code
                val mes = e.response().message
                val errorCode = e.errorResponse().code()
                message = "status: $status, message: $mes, errorCode: $errorCode"
            }
            Log.e(TAG, "Failed to create remote diff: $message", e)
            throw Exception("Failed to create remote diff: $message", e)

        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun synchronize(): Flow<SyncProgress> = flow {

        val diff = calculateDiff()
        val totalFiles = diff.totalObjects
        val totalBytes = diff.totalBytes
        var transferredFiles = 0
        diff.diff.asFlow()
            // TODO: Really another request for the content-type?
            .map { s3Obj -> s3Repository.stat(s3Obj.name) }
            .map { s3StatObj ->
                s3StatObj to localContentResolver.createOutputStream(
                    s3StatObj.`object`(),
                    s3StatObj.contentType()
                )
            }
            .onEach { transferredFiles++ } // TODO too early, should happen after the upload
            .flatMapConcat { (s3Obj, outputStream) ->
                s3Repository.get(
                    s3Obj.`object`(),
                    s3Obj.size(),
                    outputStream
                )
            }
            .runningFold(SyncProgress.EMPTY) { acc, value ->
                val sumTransferredBytes =
                    acc.transferredBytes + value.readBytes
                // Divide by 0 safe-guard in case of empty file size(s)
                val percentage = if (diff.totalBytes == 0L) 1f else sumTransferredBytes.toFloat() / diff.totalBytes
                SyncProgress(
                    transferredBytes = sumTransferredBytes,
                    transferredFiles,
                    percentage,
                    value.bytesPerSec,
                    totalFiles,
                    totalBytes
                )
            }
            .onStart {
                emit(SyncProgress.EMPTY.copy(totalFiles = totalFiles, totalBytes = totalBytes))
            }
            .sampleWithLast(samplePeriod)
            .collect { emit(it) }
    }

}

data class DownloadDiff(

    override var totalObjects: Int = 0,
    override var totalBytes: Long = 0,

    val remotes: List<S3Object>,
    val locals: List<ContentObject>,
    val diff: List<S3Object>,
) : Diff()

/**
 * Extension function that uses [Flow.sample] but ensures the last value is emitted.
 */
@OptIn(FlowPreview::class)
fun <T> Flow<T>.sampleWithLast(periodMillis: Long): Flow<T> = flow {
    require(periodMillis > 0) { "Sample period should be positive" }
    var last: T? = null
    var emitted: T? = null

    this@sampleWithLast
        .onEach { last = it }
        .sample(periodMillis)
        .collect {
            emitted = it
            emit(it)
        }

    if (last != null && last != emitted) {
        emit(last!!)
    }
}