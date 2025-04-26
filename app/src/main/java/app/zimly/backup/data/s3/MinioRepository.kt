package app.zimly.backup.data.s3

import android.util.Log
import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.GetObjectResponse
import io.minio.ListObjectsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioAsyncClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.RemoveObjectsArgs
import io.minio.messages.DeleteObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.io.InputStream
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


data class S3Object(
    var name: String,
    var size: Long,
    var checksum: String,
    var modified: ZonedDateTime,
)

class MinioRepository(
    private val url: String,
    private val key: String,
    private val secret: String,
    private val bucket: String,
    private val region: String? = null
) : S3Repository {

    private fun mc(uploadProgressTracker: ProgressTracker? = null, downloadProgressTracker: ProgressTracker? = null): MinioAsyncClient = try {
        MinioAsyncClient.builder()
            .httpClient(client(uploadProgressTracker, downloadProgressTracker))
            .endpoint(url)
            .region(region) // This might override some AWS stuff happening inside #endpoint()
            .credentials(key, secret)
            .build()
    } catch (e: Exception) {
        throw Exception("Failed to initialize S3 client: ${e.message}", e)
    }

    companion object {

        private val TAG: String? = MinioRepository::class.simpleName

        /**
         * Creates a http client with optional [UploadProgressInterceptor] or [DownloadProgressInterceptor].
         *
         * Partly taken from [io.minio.http.HttpUtils.newDefaultHttpClient]
         */
        fun client(uploadProgressTracker: ProgressTracker? = null, downloadProgressTracker: ProgressTracker? = null): OkHttpClient {
            val timeout = TimeUnit.MINUTES.toMillis(5)

            val builder = OkHttpClient()
                .newBuilder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .protocols(listOf(Protocol.HTTP_1_1))

            // attach interceptor if given
            uploadProgressTracker?.let { builder.addInterceptor(UploadProgressInterceptor(it)) } // TODO networkInterceptor?
            downloadProgressTracker?.let { builder.addInterceptor(DownloadProgressInterceptor(it)) }
            // Alternatively attach a builder#eventListenerFactory and listen to requestBodyEnd, but same problems
            // with request body returning too early. Might still be slicker than the interceptor

            return builder.build()
        }
    }


    override suspend fun bucketExists(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val param = BucketExistsArgs.builder().bucket(bucket).build()
            mc().bucketExists(param).whenComplete { result, exception ->
                if (exception == null) {
                    continuation.resume(result)
                } else {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    override fun listObjects(): List<S3Object> {
        return mc().listObjects(ListObjectsArgs.builder().bucket(bucket).recursive(true).build())
            .map { it.get() }
            .map {
                val name = it.objectName()
                val size = it.size()
                val checksum = it.etag()
                val modified = it.lastModified()
                S3Object(name, size, checksum, modified)
            }
    }

    override suspend fun createBucket(bucket: String) {
        return suspendCancellableCoroutine { continuation ->
            mc().makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
                .whenComplete { _, exception ->
                    if (exception == null) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(exception)
                    }
                }
        }
    }

    override suspend fun get(name: String): GetObjectResponse {
        return suspendCancellableCoroutine { continuation ->

            val params = GetObjectArgs.builder().bucket(bucket).`object`(name).build()

            mc().getObject(params).whenComplete { result, exception ->
                if (exception == null) {
                    continuation.resume(result)
                } else {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    fun getWithProgress(name: String, size: Long): Flow<Progress> = channelFlow {

        val progressTracker = ProgressTracker(size)
        launch {
            progressTracker.observe().collect { send(it) }
        }
        suspendCoroutine { continuation ->
            val params = GetObjectArgs.builder().bucket(bucket).`object`(name).build()

            mc(downloadProgressTracker = progressTracker).getObject(params).whenComplete { result, exception ->
                if (exception == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }


    override suspend fun remove(name: String) {
        return suspendCancellableCoroutine { continuation ->

            val params = RemoveObjectArgs.builder().bucket(bucket).`object`(name).build()

            mc().removeObject(params).whenComplete { _, exception ->
                if (exception == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    fun removeAll() {

        val delObjs = listObjects().map { DeleteObject(it.name) }

        val params = RemoveObjectsArgs.builder()
            .bucket(bucket)
            .objects(delObjs)
            .build()

        mc().removeObjects(params).forEach {
            val err = it.get()
            err?.let { Log.e(TAG, "Error deleting ${err.objectName()}: ${err.message()}") }
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
        suspendCoroutine { continuation ->

            val param = PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(name)
                .contentType(contentType)
                .stream(stream, size, -1)
                .build()

            mc(uploadProgressTracker = progressTracker).putObject(param).whenComplete { _, exception ->
                with(stream) { close() } // Instead of stream.close()
                if (exception == null) {
                    continuation.resume(Unit)
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

    suspend fun createBucket(bucket: String)
    suspend fun bucketExists(): Boolean
    suspend fun get(name: String): GetObjectResponse
    suspend fun remove(name: String)
}

