package io.zeitmaschine.zimzync.data.s3

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.takeWhile
import java.io.FilterInputStream
import java.io.InputStream
import kotlin.time.TimeSource

/**
 * Wraps an Inputstream to track the progress. Inspired by https://github.com/minio/minio-java/blob/master/examples/ProgressStream.java
 */
class ProgressStream(private val delegate: InputStream, private val progress: ProgressTracker) :
    FilterInputStream(delegate) {
    companion object {
        fun wrap(delegate: InputStream, progress: ProgressTracker): InputStream =
            ProgressStream(delegate, progress)
    }

    override fun read(): Int {
        val readBytes = delegate.read()
        this.progress.stepBy(readBytes)
        return readBytes
    }

    override fun read(toStore: ByteArray?): Int {
        val readBytes: Int = this.delegate.read(toStore)
        this.progress.stepBy(readBytes)
        return readBytes
    }

    override fun read(toStore: ByteArray?, off: Int, len: Int): Int {
        val readBytes: Int = this.delegate.read(toStore, off, len)
        this.progress.stepBy(readBytes)
        return readBytes
    }

    override fun skip(n: Long): Long {
        val readBytes = this.delegate.skip(n)
        this.progress.stepTo(readBytes)
        return readBytes
    }

}

class ProgressTracker(private val size: Long) {

    private val timeSource = TimeSource.Monotonic

    // Important: MutableStateFlow does not emit same values!
    private val progressFlow = MutableSharedFlow<ProgressIncrement>(500)
    // https://stackoverflow.com/questions/70935356/how-can-i-calculate-min-max-average-of-continuous-flow-in-kotlin
    // https://docs.oracle.com/javase/8/docs/api/java/util/DoubleSummaryStatistics.html

    fun stepBy(read: Int) {
        progressFlow.tryEmit(ProgressIncrement(read.toLong()))
    }

    fun stepTo(skipped: Long) {
        progressFlow.tryEmit(ProgressIncrement(skipped))
    }

    fun observe(): Flow<Progress> {
        return progressFlow
            .takeWhile { it.readBytes != -1L }
            .runningFold(Progress.EMPTY){ acc, inc ->
                val totalBytes = acc.totalReadBytes + inc.readBytes
                val percentage = totalBytes.toFloat() / size

                val timeMark = timeSource.markNow()
                var duration = (timeMark - acc.timeMark).inWholeMicroseconds

                // Divide by zero safe guard,
                duration = if (duration > 0) duration else 1

                // Bytes/µs to Bytes/s
                val bytesPerSec = inc.readBytes * 1000_000 / duration
                Progress(inc.readBytes, totalBytes, percentage, size, bytesPerSec, timeMark)
            }
    }

}

private data class ProgressIncrement(val readBytes: Long, val timestamp: Long = System.currentTimeMillis())
data class Progress(val readBytes: Long, val totalReadBytes: Long, val percentage: Float, val size: Long, val bytesPerSec: Long, val timeMark: TimeSource.Monotonic.ValueTimeMark) {
    companion object {
        val EMPTY = Progress(0, 0, 0F, 0, 0, TimeSource.Monotonic.markNow())
    }
}
