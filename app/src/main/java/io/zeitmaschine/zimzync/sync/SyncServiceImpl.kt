package io.zeitmaschine.zimzync.sync

import android.util.Log
import io.zeitmaschine.zimzync.data.media.MediaObject
import io.zeitmaschine.zimzync.data.media.MediaRepository
import io.zeitmaschine.zimzync.data.s3.S3Object
import io.zeitmaschine.zimzync.data.s3.S3Repository
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
    private val mediaRepository: MediaRepository
) :
    SyncService {

    companion object {
        val TAG: String? = SyncServiceImpl::class.simpleName
    }

    override fun diff(buckets: Set<String>): Diff {
        try {
            val remotes = s3Repository.listObjects()
            val photos = mediaRepository.getMedia(buckets)
            val diff = photos.filter { local -> remotes.none { remote -> remote.name == local.name } }
            val size = diff.sumOf { it.size }

            return Diff(remotes, photos, diff, size)
        } catch (e: Exception) {
            Log.e(TAG, "${e.message}")
            throw Exception("Failed to create diff: ${e.message}", e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun sync(diff: Diff, debounce: Long): Flow<SyncProgress> {
        var count = 0
        return diff.diff.asFlow()
            .map { mediaObj -> Pair(mediaObj, mediaRepository.getStream(mediaObj.path)) }
            .onEach { count++ } // TODO too early, should happen after the upload
            .flatMapConcat { (mediaObj, file) ->
                s3Repository.put(
                    file,
                    mediaObj.name,
                    mediaObj.contentType,
                    mediaObj.size)
            }
            .runningFold(SyncProgress.EMPTY) {acc, value ->
                val totalBytes = acc.readBytes + value.readBytes // Don't sum on totalbytes, double counts
                val percentage = totalBytes.toFloat() / diff.size
                SyncProgress(readBytes = totalBytes, count, percentage)
            }.debounce(debounce)
    }
}
data class SyncProgress(val readBytes: Long, val uploadedFiles: Int, val percentage: Float) {
    companion object {
        val EMPTY = SyncProgress(0, 0, 0F)
    }
}

data class Diff(val remotes: List<S3Object>, val locals: List<MediaObject>, val diff: List<MediaObject>, val size: Long) {
    companion object {
        val EMPTY = Diff(emptyList(), emptyList(), emptyList(), 0)
    }
}

interface SyncService {

    fun diff(buckets: Set<String>): Diff
    fun sync(diff: Diff, debounce: Long = 0L): Flow<SyncProgress>
}

