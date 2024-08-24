package io.zeitmaschine.zimzync.data.s3

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer

/**
 * Interceptor for okhttp client that wraps the RequestBody of PUT requests to emit the read/uploaded data to
 * the [ProgressTracker]. Only emits the [ProgressRequestBody.read] value upon successful response to
 * take the network transfer into account.
 *
 * Refs:
 * https://medium.com/@PaulinaSadowska/display-progress-of-multipart-request-with-retrofit-and-rxjava-23a4a779e6ba
 * https://getstream.io/blog/android-upload-progress/
 * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/Progress.java
 */
internal class ProgressInterceptor(
    private val progressTracker: ProgressTracker,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = wrapRequest(chain.request())
        val res = chain.proceed(req)
        if (req.body is ProgressRequestBody && res.isSuccessful) {
            progressTracker.stepBy((req.body as ProgressRequestBody).read)
        }

        return res
    }

    private fun wrapRequest(request: Request): Request {
        return if (request.body != null && request.method == "PUT")
            request.newBuilder()
                .put(ProgressRequestBody(request.body!!))
                .build()
        else request
    }
}

internal class ProgressRequestBody(
    private val delegate: RequestBody,
) : RequestBody() {

    var read: Long = 0
        private set

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
            read += byteCount
        }
    }

}