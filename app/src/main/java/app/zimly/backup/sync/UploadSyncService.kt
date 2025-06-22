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
import kotlinx.coroutines.flow.flow
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

        /**
         * Determines which local [ContentObject]s should be uploaded based on remote presence and modification time.
         *
         * A local object is selected if:
         * * It does not exist remotely (by matching [ContentObject.relPath] to [S3Object.name]), or
         * * Its local [ContentObject.lastModified] is more recent than the corresponding remote [S3Object.modified].
         *
         * @param locals The list of local [ContentObject]s.
         * @param remotes The list of remote [S3Object]s.
         * @return A list of local [ContentObject]s to upload.
         */
        fun calculateUploads(
            locals: List<ContentObject>,
            remotes: List<S3Object>
        ): List<ContentObject> {

            val remotesByName = remotes.associateBy { it.name }

            return locals.filter { local ->
                val remote = remotesByName[local.relPath]
                val isNewOrUpdatedLocal = remote == null || local.lastModified > remote.modified.toInstant().toEpochMilli()
                isNewOrUpdatedLocal
            }
        }
    }

    override fun calculateDiff(): UploadDiff {
        try {
            val remotes = s3Repository.listObjects()
            val objects = localContentResolver.listObjects()

            val uploads = calculateUploads(objects, remotes)
            val totalBytes = uploads.sumOf { it.size }

            return UploadDiff(uploads.size, totalBytes, remotes, objects, uploads)
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
    override fun synchronize(): Flow<SyncProgress> = flow {
        val diff = calculateDiff()
        val totalFiles = diff.totalObjects
        val totalBytes = diff.totalBytes
        var transferredFiles = 0
        diff.diff.asFlow()
            .map { mediaObj -> Pair(mediaObj, localContentResolver.getInputStream(mediaObj.uri)) }
            .onEach { transferredFiles++ } // TODO too early, should happen after the upload
            .flatMapConcat { (mediaObj, file) ->
                s3Repository.put(
                    file,
                    mediaObj.path,
                    mediaObj.contentType,
                    mediaObj.size
                )
            }
            .runningFold(SyncProgress.EMPTY) { acc, value ->
                val sumTransferredBytes =
                    acc.transferredBytes + value.readBytes
                // Divide by 0 safe-guard in case of empty file size(s)
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
            .debounce(debounce)
            .collect { emit(it) }
    }
}

data class UploadDiff(

    override var totalObjects: Int = 0,
    override var totalBytes: Long = 0,

    val remotes: List<S3Object>,
    val locals: List<ContentObject>,
    val diff: List<ContentObject>,
) : Diff()

