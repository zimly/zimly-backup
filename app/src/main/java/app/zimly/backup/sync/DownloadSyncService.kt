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
    }

    override fun calculateDiff(): DownloadDiff {
        try {
            val remotes = s3Repository.listObjects()
            val objects = localContentResolver.listObjects()
            val diff =
                remotes.filter { remote -> objects.none { obj -> obj.relPath == remote.name } }
            val size = diff.sumOf { it.size }
            return DownloadDiff(diff.size, size, remotes, objects, diff)
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
                val percentage = sumTransferredBytes.toFloat() / diff.totalBytes
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