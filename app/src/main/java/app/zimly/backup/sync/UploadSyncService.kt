package app.zimly.backup.sync

import android.util.Log
import app.zimly.backup.data.media.ContentObject
import app.zimly.backup.data.media.LocalContentResolver
import app.zimly.backup.data.s3.S3Object
import app.zimly.backup.data.s3.S3Repository
import io.minio.errors.ErrorResponseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold

class UploadSyncService(
    private val s3Repository: S3Repository,
    private val localContentResolver: LocalContentResolver,
    private val debounce: Long = 0L
) :
    SyncService {

    companion object {
        val TAG: String? = UploadSyncService::class.simpleName
    }

    fun calculateDiff(): UploadDiff {
        try {
            val remotes = s3Repository.listObjects()
            val objects = localContentResolver.listObjects()
            val diff =
                objects.filter { local -> remotes.none { remote -> remote.name == local.name } }
            val size = diff.sumOf { it.size }

            return UploadDiff(remotes, objects, diff, size)
        } catch (e: Exception) {
            var message = e.message
            if (e is ErrorResponseException) {
                val status = e.response().code
                val mes = e.response().message
                val errorCode = e.errorResponse().code()
                message = "status: $status, message: $mes, errorCode: $errorCode"
            }
            Log.e(TAG, "Failed to create local diff: $message", e)
            throw Exception("Failed to create local diff: $message", e)

        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun synchronize(): Flow<SyncProgress> {
        val diff = calculateDiff() // TODO: Error handling!
        val totalFiles = diff.diff.size
        val totalBytes = diff.size
        var transferredFiles = 0
        return diff.diff.asFlow()
            .map { mediaObj -> Pair(mediaObj, localContentResolver.getInputStream(mediaObj.path)) }
            .onEach { transferredFiles++ } // TODO too early, should happen after the upload
            .flatMapConcat { (mediaObj, file) ->
                s3Repository.put(
                    file,
                    mediaObj.name,
                    mediaObj.contentType,
                    mediaObj.size
                )
            }
            .runningFold(SyncProgress.EMPTY) { acc, value ->
                val sumTransferredBytes =
                    acc.transferredBytes + value.readBytes
                val percentage = sumTransferredBytes.toFloat() / diff.size
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
            .debounce(debounce)
    }
}

data class UploadDiff(
    val remotes: List<S3Object>,
    val locals: List<ContentObject>,
    val diff: List<ContentObject>,
    val size: Long
)

