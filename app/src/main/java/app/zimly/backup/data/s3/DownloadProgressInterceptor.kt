package app.zimly.backup.data.s3

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

/**
 * Interceptor for okhttp client that wraps the ResponseBody to emit the read/downloaded data to
 * the [ProgressTracker].
 */
internal class DownloadProgressInterceptor(
    private val progressTracker: ProgressTracker,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())

        if (originalResponse.body != null)
            return originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body!!, progressTracker))
                .build()

        return originalResponse
    }
}

internal class ProgressResponseBody(
    private val delegate: ResponseBody,
    private val progressTracker: ProgressTracker
) : ResponseBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun source(): BufferedSource {
        return CountingSource(delegate.source()).buffer()
    }

    override fun contentLength(): Long = delegate.contentLength()

    private inner class CountingSource(delegate: Source) : ForwardingSource(delegate) {

        override fun read(sink: Buffer, byteCount: Long): Long {
            var bytesRead = super.read(sink, byteCount)
            if (bytesRead > -1) {
                progressTracker.stepBy(bytesRead) // TODO: Not here in Put* because of reasons other than waiting for the wire??
            }
            return bytesRead
        }
    }

}
