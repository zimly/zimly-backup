package io.zeitmaschine.zimzync.sync

import android.util.Log
import io.zeitmaschine.zimzync.data.media.MediaObject
import io.zeitmaschine.zimzync.data.media.MediaRepository
import io.zeitmaschine.zimzync.data.s3.Progress
import io.zeitmaschine.zimzync.data.s3.S3Object
import io.zeitmaschine.zimzync.data.s3.S3Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMap
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onErrorResume
import kotlinx.coroutines.flow.reduce

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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun sync(diff: Diff, progress: (size: Long) -> Unit): Flow<Progress> {

        return diff.diff.asFlow()
            .map { mediaObj -> Pair(mediaObj, mediaRepository.getStream(mediaObj.path)) }
            .flatMapConcat { (mediaObj, file) ->
                s3Repository.put(
                    file,
                    mediaObj.name,
                    mediaObj.contentType,
                    mediaObj.size
                )}
            // TODO .reduce(total)
            .catch { Log.e(TAG, "Failed to sync diff.", it) } // TODO verify exception handling
    }

}

data class Diff(val remotes: List<S3Object>, val locals: List<MediaObject>, val diff: List<MediaObject>, val size: Long) {
    companion object {
        val EMPTY = Diff(emptyList(), emptyList(), emptyList(), 0)
    }
}

interface SyncService {

    fun diff(buckets: Set<String>): Diff
    fun sync(diff: Diff, progress: (size: Long) -> Unit): Flow<Progress>
}

