package io.zeitmaschine.zimzync

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

class SyncServiceImpl(
    private val s3Repository: S3Repository,
    private val mediaRepository: MediaRepository
) :
    SyncService {

    companion object {
        val TAG: String? = SyncServiceImpl::class.simpleName
    }

    override suspend fun diff(): Result<Diff> {
        // Move the execution of the coroutine to the I/O dispatcher
        return withContext(Dispatchers.IO) {
            try {
                val remotes = s3Repository.listObjects()
                val photos = mediaRepository.getPhotos()
                val diff = photos.filter { local -> remotes.none { remote -> remote.name == local.name } }

                val data = Diff(remotes, photos, diff)
                return@withContext Result.Success(data)
            } catch (e: Exception) {
                Log.i(TAG, "${e.message}")
                return@withContext Result.Error(e)
            }
        }
    }


    override fun diffA(): Diff {
        // Move the execution of the coroutine to the I/O dispatcher
        try {
            val remotes = s3Repository.listObjects()
            val photos = mediaRepository.getPhotos()
            val diff = photos.filter { local -> remotes.none { remote -> remote.name == local.name } }

            return Diff(remotes, photos, diff)
        } catch (e: Exception) {
            Log.i(TAG, "${e.message}")
            throw Exception("ups", e)
        }
    }

    override fun sync(diff: Diff, progress: (size: Long) -> Unit) {

        // Move the execution of the coroutine to the I/O dispatcher
        try {
            diff.diff
                .map { mediaObj -> Pair(mediaObj, mediaRepository.getStream(mediaObj.path)) }
                .forEach { (mediaObj, file) ->
                    s3Repository.put(
                        file,
                        mediaObj.name,
                        mediaObj.contentType,
                        mediaObj.size
                    )
                    progress(mediaObj.size)
                }
        } catch (e: Exception) {
            Log.e(TAG, "${e.message}")
            throw Exception("Failed to sync files: ${e.message}", e)
        }
    }

}

data class Diff(val remotes: List<S3Object>, val locals: List<MediaObject>, val diff: List<MediaObject>) {
    companion object {
        val EMPTY = Diff(emptyList(), emptyList(), emptyList())
    }
}

interface SyncService {

    suspend fun diff(): Result<Diff>
    fun diffA(): Diff
    fun sync(diff: Diff, progress: (size: Long) -> Unit)
}

