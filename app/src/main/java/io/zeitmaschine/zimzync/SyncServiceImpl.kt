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
                val s3Objects = s3Repository.listObjects()
                val photos = mediaRepository.getPhotos()
                val data = Diff(s3Objects, photos)
                return@withContext Result.Success(data)
            } catch (e: Exception) {
                Log.i(TAG, "${e.message}")
                return@withContext Result.Error(e)
            }
        }
    }

    override suspend fun sync(diff: Diff): Result<Boolean> {
        // Move the execution of the coroutine to the I/O dispatcher
        return withContext(Dispatchers.IO) {
            try {
                diff.locals
                    .filter { loc -> diff.remotes.none { rem -> rem.name == loc.name } }
                    .map { loc -> Pair(loc, mediaRepository.getStream(loc.path)) }
                    .forEach { (loc, file) -> s3Repository.put(file, loc.name, loc.contentType, loc.size) }

                return@withContext Result.Success(true)
            } catch (e: Exception) {
                Log.i(TAG, "${e.message}")
                return@withContext Result.Error(e)
            }
        }
    }

}

data class Diff(val remotes: List<S3Object>, val locals: List<MediaObject>)

interface SyncService {

    suspend fun diff(): Result<Diff>
    suspend fun sync(diff: Diff): Result<Boolean>
}

