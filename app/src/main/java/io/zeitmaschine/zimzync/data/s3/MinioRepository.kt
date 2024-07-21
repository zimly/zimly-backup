package io.zeitmaschine.zimzync.data.s3

import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.GetObjectResponse
import io.minio.ListObjectsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioAsyncClient
import io.minio.ObjectWriteResponse
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.http.HttpUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
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
    private val bucket: String
) : S3Repository {

    private fun mc(progressTracker: ProgressTracker? = null): MinioAsyncClient = try {
        MinioAsyncClient.builder()
            .httpClient(client(progressTracker))
            .endpoint(url)
            .credentials(key, secret)
            .build()
    } catch (e: Exception) {
        throw Exception("Failed to initialize S3 client: ${e.message}", e)
    }

    // Copied from io.minio.http.HttpUtils.newDefaultHttpClient
    private fun client(progressTracker: ProgressTracker?): OkHttpClient {
        val timeout = TimeUnit.MINUTES.toMillis(5)

            val builder = OkHttpClient()
                .newBuilder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .protocols(listOf(Protocol.HTTP_1_1))

        progressTracker?.let { builder.addInterceptor(ProgressInterceptor(it)) }
        var httpClient = builder.build()
        val filename = System.getenv("SSL_CERT_FILE")
        if (filename != null && filename.isNotEmpty()) {
            try {
                httpClient = HttpUtils.enableExternalCertificates(httpClient, filename)
            } catch (e: GeneralSecurityException) {
                throw RuntimeException(e)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        return httpClient
    }

    override suspend fun verify(): Boolean {
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
        doPut(stream, name, contentType, size, progressTracker)
    }.transformWhile {
        emit(it)
        it.percentage < 1F
    }

    private suspend fun doPut(
        stream: InputStream,
        name: String,
        contentType: String,
        size: Long,
        progressTracker: ProgressTracker
    ): ObjectWriteResponse {


        return suspendCoroutine { continuation ->

            val param = PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(name)
                .contentType(contentType)
                .stream(stream, size, -1)
                .build()

            mc(progressTracker).putObject(param).whenComplete { result, exception ->
                with(stream) { close() } // Instead of stream.close()
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

    suspend fun createBucket(bucket: String)
    suspend fun verify(): Boolean
    suspend fun get(name: String): GetObjectResponse
    suspend fun remove(name: String)
}

/**
 * https://medium.com/@PaulinaSadowska/display-progress-of-multipart-request-with-retrofit-and-rxjava-23a4a779e6ba
 * https://getstream.io/blog/android-upload-progress/
 * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/Progress.java
 */
internal class ProgressInterceptor(private val progressTracker: ProgressTracker) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var req = chain.request()
        if (req.method == "PUT")
            req = wrapRequest(req, progressTracker)
        return chain.proceed(req)
    }

    private fun wrapRequest(request: Request, progressCallback: ProgressTracker): Request {
        return request.newBuilder()
            // Assume that any request tagged with a ProgressCallback is a POST
            // request and has a non-null body
            .put(ProgressRequestBody(request.body!!, progressCallback))
            .build()
    }
}

internal class ProgressRequestBody(
    private val delegate: RequestBody,
    private val progressTracker: ProgressTracker
) : RequestBody() {
    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink).buffer()
        delegate.writeTo(countingSink)
        countingSink.flush()
    }

    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            progressTracker.stepBy(byteCount.toInt())
        }
    }

}

