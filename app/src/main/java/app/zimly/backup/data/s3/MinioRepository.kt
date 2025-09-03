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
import io.minio.StatObjectArgs
import io.minio.StatObjectResponse
import io.minio.messages.DeleteObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


data class S3Object(
    /**
     * S3 object name is the path.
     */
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
    private val region: String? = null,
    private val virtualHostedStyle: Boolean = false
) : S3Repository {

    /**
     * [virtualHostedStyle] is a huge mess in the underlying SDK, see
     * [io.minio.S3Base.buildUrl] and [io.minio.MinioAsyncClient.Builder.endpoint].
     *
     * Gist is (I think):
     *   * AWS and aliyuncs.com endpoints are automagically set to use virtualHostedStyle
     *   * Tencent and other providers need to set this explicitly if needed, but it's not exposed
     *     through the builder.
     *   * The bucket name needs to be stripped of the URL, and the region configured explicitly:
     *     It automagically prefixes the bucket in the URL. If a vendor does not prefix the bucket name, but
     *     relies on virtual host style, things won't work.
     *   * Minio Java SDK is .. bad :(
     *
     */
    private fun mc(uploadProgressTracker: ProgressTracker? = null): MinioAsyncClient {
        try {
            val client = MinioAsyncClient.builder()
                .httpClient(client(uploadProgressTracker))
                .endpoint(url)
                .region(region) // This might override some AWS stuff happening inside #endpoint()
                .credentials(key, secret)
                .build()

            if (virtualHostedStyle) {
                client.enableVirtualStyleEndpoint()
            }
            return client
        } catch (e: Exception) {
            throw Exception("Failed to initialize S3 client: ${e.message}", e)
        }
    }

    companion object {

        private val TAG: String? = MinioRepository::class.simpleName

        /**
         * Creates a http client with optional [UploadProgressInterceptor].
         *
         * Partly taken from [io.minio.http.HttpUtils.newDefaultHttpClient]
         */
        fun client(uploadProgressTracker: ProgressTracker? = null): OkHttpClient {
            val timeout = TimeUnit.MINUTES.toMillis(5)

            val builder = OkHttpClient()
                .newBuilder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .protocols(listOf(Protocol.HTTP_1_1))

            // attach interceptor if given
            uploadProgressTracker?.let { builder.addInterceptor(UploadProgressInterceptor(it)) } // TODO networkInterceptor?
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

    override suspend fun stat(name: String): StatObjectResponse {
        return suspendCancellableCoroutine { continuation ->

            val params = StatObjectArgs.builder().`object`(name).bucket(bucket).build()

            mc().statObject(params).whenComplete { result, exception ->
                if (exception == null) {
                    continuation.resume(result)
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

    /**
     * Downloads a file from S3 and tracks the [Progress].
     *
     * Important: It tracks the progress when writing to the [OutputStream] instead of intercepting
     * it on network traffic. Otherwise the actual bytes read and expected are off, possibly due
     * to gzip or similar.
     * It's also important to note that in this case, network latency is already accounted for, which
     * is not the case for [put], hence we have to use the okio interceptor there.
     *
     * It's also important to close the passed stream here.
     */
    override suspend fun get(name: String, size: Long, stream: OutputStream): Flow<Progress> =
        channelFlow {
            val progressTracker = ProgressTracker(size)

            // Launch progress observer
            val progressJob = launch {
                progressTracker.observe().collect { send(it) }
            }

            // Suspend until the Minio getObject call completes or fails
            val response = suspendCancellableCoroutine<GetObjectResponse> { continuation ->
                val params = GetObjectArgs.builder().bucket(bucket).`object`(name).build()
                val future = mc().getObject(params)

                future.whenComplete { result, exception ->
                    if (exception != null) {
                        continuation.resumeWithException(exception)
                    } else {
                        continuation.resume(result)
                    }
                }

                continuation.invokeOnCancellation {
                    future.cancel(true)
                }
            }

            // Now perform the actual download on IO dispatcher
            val downloadJob = launch(Dispatchers.IO) {
                try {
                    // use { } closes the sink and stream
                    response.source().use { rawSource ->
                        val countedSource = rawSource.counted(progressTracker)
                        stream.use { os ->
                            os.sink().buffer().use { sink ->
                                // do not waste I/O resources on cancelled work
                                ensureActive()
                                sink.writeAll(countedSource)
                                sink.flush()
                            }
                        }
                    }
                } catch (e: Exception) {
                    cancel("Download failed", e)
                }
            }

            // Wait for download to finish
            downloadJob.join()

            // Wait for progress to finish naturally (progressTracker.observe completes on 100%)
            progressJob.join()
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

            mc(uploadProgressTracker = progressTracker).putObject(param)
                .whenComplete { _, exception ->
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
    suspend fun stat(name: String): StatObjectResponse
    suspend fun get(name: String, size: Long, outputStream: OutputStream): Flow<Progress>
    suspend fun remove(name: String)
}

/**
 * Wraps the [Source] in a [ForwardingSource] that emits the bytes read to the passed [ProgressTracker].
 */
fun Source.counted(tracker: ProgressTracker): Source {
    return object : ForwardingSource(this) {
        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            if (bytesRead > 0) {
                tracker.stepBy(bytesRead)
            }
            return bytesRead
        }
    }.buffer()
}
