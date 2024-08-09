package io.zeitmaschine.zimzync.data.s3

import androidx.compose.runtime.produceState
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
 * Interceptor for okhttp client that wraps the RequestBody of PUT requests to track/buffer the read/uploaded data to
 * the [ProgressTracker]. Upon successful response it will commit/flush the read buffer. Ensuring
 * that the upload was successful and the real time consumption for the upload is taken into account.
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
        val res = chain.proceed(req)
        //if (req.method == "PUT" && res.isSuccessful)
            //progressTracker.stepBy((req.body as ProgressRequestBody).read)
        return res
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
    var read: ProgressTracker
) : RequestBody() {
    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink, read).buffer()
        delegate.writeTo(countingSink)
        countingSink.flush()
    }

    private inner class CountingSink(delegate: Sink, val tracker: ProgressTracker) : ForwardingSink(delegate) {

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            tracker.stepBy(byteCount)
        }
    }

}