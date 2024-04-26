package io.zeitmaschine.zimzync.data.s3

import android.util.Log
import io.minio.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    private var mc: MinioClient

    init {
        try {
            mc = MinioClient.builder()
                .endpoint(url)
                .credentials(key, secret)
                .build()
        } catch (e: Exception) {
            throw Exception("Failed to initialize minio client: ${e.message}", e)
        }
    }

    override fun verify(): Boolean {
        try {
            return mc.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
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
        mc.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
    }

    override fun put(stream: InputStream, name: String, contentType: String, size: Long): Flow<Progress> {
        val progress = ProgressTracker(size)
        runBlocking { // TODO :thinking_face:
            launch {
                ProgressStream.wrap(stream, progress).use {
                    val param = PutObjectArgs.builder()
                        .bucket(bucket)
                        .`object`(name)
                        .contentType(contentType)
                        .stream(it, size, -1)
                        .build()
                    Log.i(TAG, "Uploading $name")
                    mc.putObject(param)
                }
            }
        }
        return progress.observe()
    }
}

interface S3Repository {
    fun listObjects(): List<S3Object>
    fun put(stream: InputStream, name: String, contentType: String, size: Long): Flow<Progress>
    fun createBucket(bucket: String)
    fun verify(): Boolean
}