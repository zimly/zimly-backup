package app.zimly.backup.sync

import android.util.Log
import io.minio.errors.ErrorResponseException
import app.zimly.backup.data.media.ContentObject
import app.zimly.backup.data.media.LocalContentResolver
import app.zimly.backup.data.s3.S3Object
import app.zimly.backup.data.s3.S3Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold

class SyncServiceImpl(
    private val s3Repository: S3Repository,
    private val localContentResolver: LocalContentResolver
) :
    SyncService {

    companion object {
        val TAG: String? = SyncServiceImpl::class.simpleName
    }

    override fun localDiff(): LocalDiff {
        try {
            val remotes = s3Repository.listObjects()
            val objects = localContentResolver.listObjects()
            val diff =
                objects.filter { local -> remotes.none { remote -> remote.name == local.name } }
            val size = diff.sumOf { it.size }

            return LocalDiff(remotes, objects, diff, size)
        } catch (e: Exception) {
            var message = e.message
            if (e is ErrorResponseException) {
                val status = e.response().code
                val mes = e.response().message
                val errorCode = e.errorResponse().code()
                message = "status: $status, message: $mes, errorCode: $errorCode"
            }
            Log.e(TAG, "Failed to create diff: $message", e)
            throw Exception("Failed to create diff: $message", e)

        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun upload(diff: LocalDiff, debounce: Long): Flow<SyncProgress> {
        var count = 0
        return diff.diff.asFlow()
            .map { mediaObj -> Pair(mediaObj, localContentResolver.getStream(mediaObj.path)) }
            .onEach { count++ } // TODO too early, should happen after the upload
            .flatMapConcat { (mediaObj, file) ->
                s3Repository.put(
                    file,
                    mediaObj.name,
                    mediaObj.contentType,
                    mediaObj.size
                )
            }
            .runningFold(SyncProgress.EMPTY) { acc, value ->
                val totalBytes =
                    acc.readBytes + value.readBytes // Don't sum on totalbytes, double counts
                val percentage = totalBytes.toFloat() / diff.size
                SyncProgress(readBytes = totalBytes, count, percentage, value.bytesPerSec)
            }
            .debounce(debounce)
    }
}

data class SyncProgress(
    val readBytes: Long,
    val uploadedFiles: Int,
    val percentage: Float,
    val bytesPerSecond: Long?
) {
    companion object {
        val EMPTY = SyncProgress(0, 0, 0F, null)
    }
}

data class LocalDiff(
    val remotes: List<S3Object>,
    val locals: List<ContentObject>,
    val diff: List<ContentObject>,
    val size: Long
)

interface SyncService {
    fun localDiff(): LocalDiff
    fun upload(diff: LocalDiff, debounce: Long = 0L): Flow<SyncProgress>
}

