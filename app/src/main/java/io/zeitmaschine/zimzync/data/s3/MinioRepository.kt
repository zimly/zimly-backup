package io.zeitmaschine.zimzync.data.s3

import android.util.Log
import io.minio.BucketExistsArgs
import io.minio.ListObjectsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioAsyncClient
import io.minio.PutObjectArgs
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.time.ZonedDateTime


data class S3Object(
    var name: String,
    var size: Long,
    var checksum: String,
    var modified: ZonedDateTime,
)

class MinioRepository(url: String, key: String, secret: String, private val bucket: String) :
    S3Repository {

    companion object {
        val TAG: String? = MinioRepository::class.simpleName
    }

    private var mc: MinioAsyncClient

    init {
        try {
            mc = MinioAsyncClient.builder()
                .endpoint(url)
                .credentials(key, secret)
                .build()
        } catch (e: Exception) {
            throw Exception("Failed to initialize minio client: ${e.message}", e)
        }
    }

    override fun verify(): Boolean {
        try {
            return mc.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()).get();
        } catch (e: Exception) {
            throw Exception("Failed to connect to minio.", e)
        }
    }

    override fun listObjects(): List<S3Object> {
        return mc.listObjects(ListObjectsArgs.builder().bucket(bucket).recursive(true).build())
            .map { it.get() }
            .map {
                val name = it.objectName()
                val size = it.size()
                val checksum = it.etag()
                val modified = it.lastModified()
                S3Object(name, size, checksum, modified)
            }
    }

    override fun createBucket(bucket: String) {
        mc.makeBucket(MakeBucketArgs.builder().bucket(bucket).build()).get()
    }

    override fun put(stream: InputStream, name: String, contentType: String, size: Long): Flow<ObjectProgress> {
        val progress = ProgressTracker(size)
        ProgressStream.wrap(stream, progress).use {
            val param = PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(name)
                .contentType(contentType)
                .stream(it, size, -1)
                .build()
            Log.i(TAG, "Uploading $name")
            // non-blocking
            mc.putObject(param).get()
        }
        return progress.observe()
    }
}

interface S3Repository {
    fun listObjects(): List<S3Object>
    fun put(stream: InputStream, name: String, contentType: String, size: Long): Flow<ObjectProgress>
    fun createBucket(bucket: String)
    fun verify(): Boolean
}