package io.zeitmaschine.zimzync

import android.util.Log
import io.minio.BucketExistsArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.credentials.MinioClientConfigProvider.McConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

class MinioRepository(url: String, key: String, secret: String, private val bucket: String) {
    companion object {
        val TAG: String? = MinioRepository::class.simpleName
    }

    private lateinit var mc: MinioClient
    init {
        try {
            mc = MinioClient.builder()
                .endpoint(url)
                .credentials(key, secret)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "${e.message}")
        }
    }

    suspend fun listObjects(): Result<Boolean>{

        // Move the execution of the coroutine to the I/O dispatcher
        return withContext(Dispatchers.IO) {
            // Blocking network request code
            try {
                // Create a minioClient with the MinIO server playground, its access key and secret key.

                mc.listObjects(ListObjectsArgs.builder().bucket(bucket).build())
                    .forEach { obj -> Log.i(TAG, obj.get().objectName()) };

                return@withContext Result.Success(true)
            } catch (e: Exception) {
                Log.i(TAG, "${e.message}")
                return@withContext Result.Error(e)
            }
        }
    }

    suspend fun listBuckets(): Result<List<String>>{

        // Move the execution of the coroutine to the I/O dispatcher
        return withContext(Dispatchers.IO) {
            // Blocking network request code
            try {
                // Create a minioClient with the MinIO server playground, its access key and secret key.

                val found = mc.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!found) {
                    Log.i(SyncModel.TAG, "Bucket doesn't exists.");
                } else {
                    Log.i(SyncModel.TAG, "Bucket already exists.");
                }
                val buckets: List<String> = mc.listBuckets().map { bucket -> bucket.name() }
                return@withContext Result.Success(buckets)
            } catch (e: Exception) {
                Log.i(SyncModel.TAG, "${e.message}")
                return@withContext Result.Error(e)
            }
        }
    }

}