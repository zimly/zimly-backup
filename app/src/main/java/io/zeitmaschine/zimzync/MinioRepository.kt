package io.zeitmaschine.zimzync

import android.util.Log
import io.minio.BucketExistsArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient

data class S3Object(
    var name: String = "",
)

class MinioRepository(url: String, key: String, secret: String, private val bucket: String) :
    S3Repository {

    private var mc: MinioClient

    init {
        try {
            mc = MinioClient.builder()
                .endpoint(url)
                .credentials(key, secret)
                .build()
            verify()
        } catch (e: Exception) {
            throw Exception("Failed to initialize minio client: ${e.message}", e)
        }
    }

    private fun verify() {
        val found = mc.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            Log.i(SyncModel.TAG, "Bucket doesn't exists.");
        } else {
            Log.i(SyncModel.TAG, "Bucket already exists.");
        }
    }

    override fun listObjects(): List<S3Object> {
        // Create a minioClient with the MinIO server playground, its access key and secret key.
        return mc.listObjects(ListObjectsArgs.builder().bucket(bucket).build())
            .map { res -> S3Object(res.get().objectName()) }
    }

}

interface S3Repository {
    fun listObjects(): List<S3Object>
}