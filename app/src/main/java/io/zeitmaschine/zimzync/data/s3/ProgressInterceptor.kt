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
 * the [ProgressTracker].
 *
 * Refs:
 * https://medium.com/@PaulinaSadowska/display-progress-of-multipart-request-with-retrofit-and-rxjava-23a4a779e6ba
 * https://getstream.io/blog/android-upload-progress/
 * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/Progress.java
 */
internal class ProgressInterceptor(private val progressTracker: ProgressTracker) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var req = chain.request()
        if (req.method == "PUT")
            req = wrapRequest(req)
        return chain.proceed(req)
    }

    private fun wrapRequest(request: Request): Request {
        return request.newBuilder()
            // Assume non-null as we're only wrapping PUT requests
            .put(ProgressRequestBody(request.body!!, progressTracker))
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