package io.zeitmaschine.zimzync.data.s3

import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.GetObjectResponse
import io.minio.ListObjectsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioAsyncClient
import io.minio.ObjectWriteResponse
import io.minio.PutObjectArgs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import java.time.ZonedDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


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
            return mc.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()).get()
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

    override suspend fun get(name: String): GetObjectResponse {
        return suspendCancellableCoroutine { continuation ->

            val params = GetObjectArgs.builder().bucket(bucket).`object`(name).build()

            mc.getObject(params).whenComplete { result, exception ->
                if (exception == null) {
                    continuation.resume(result)
                } else {
                    continuation.resumeWithException(exception)
                }
            }

        }
    }

    override suspend fun put(
        stream: InputStream,
        name: String,
        contentType: String,
        size: Long
    ): Flow<Progress> = channelFlow {

        val progressTracker = ProgressTracker(size)
        launch {
            progressTracker.observe().collect { send(it) }
        }
        doPut(ProgressStream.wrap(stream, progressTracker), name, contentType, size)
    }.transformWhile {
        emit(it)
        it.percentage < 1F
    }

    private suspend fun doPut(
        stream: InputStream,
        name: String,
        contentType: String,
        size: Long
    ): ObjectWriteResponse {


        return suspendCancellableCoroutine { continuation ->
            // stream.use { ss -> // // Autoclosing!!!
            val param = PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(name)
                .contentType(contentType)
                .stream(stream, size, -1)
                .build()

            mc.putObject(param).whenComplete { result, exception ->
                if (exception == null) {
                    continuation.resume(result)
                } else {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

}

interface S3Repository {
    fun listObjects(): List<S3Object>
    suspend fun put(
        stream: InputStream,
        name: String,
        contentType: String,
        size: Long
    ): Flow<Progress>

    fun createBucket(bucket: String)
    fun verify(): Boolean
    suspend fun get(name: String): GetObjectResponse
}