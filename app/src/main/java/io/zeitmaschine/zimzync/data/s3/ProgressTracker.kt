package io.zeitmaschine.zimzync.data.s3

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.takeWhile
import java.io.FilterInputStream
import java.io.InputStream

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

    // Important: MutableStateFlow does not emit same values!
    private val progressFlow = MutableSharedFlow<Long>(500)
    // https://stackoverflow.com/questions/70935356/how-can-i-calculate-min-max-average-of-continuous-flow-in-kotlin
    // https://docs.oracle.com/javase/8/docs/api/java/util/DoubleSummaryStatistics.html

    fun stepBy(read: Int) {
        val emit = progressFlow.tryEmit(read.toLong())
        println("read stream $read, emitted: $emit")
    }

    fun stepTo(skipped: Long) {
        val emit = progressFlow.tryEmit(skipped)
        println("read stream $skipped, emitted: $emit")
    }

    fun observe(): Flow<Progress> {
        return progressFlow
            .takeWhile { it != -1L }
            .onEach { println("read flow $it") }
            .runningFold(Progress.EMPTY){ acc, value ->
                val totalBytes = acc.totalReadBytes + value
                val percentage = totalBytes.toFloat() / size
                Progress(readBytes = value, totalBytes, percentage = percentage, size)
            }
    }

}

data class Progress(val readBytes: Long, val totalReadBytes: Long, val percentage: Float, val size: Long) {
    companion object {
        val EMPTY = Progress(0, 0, 0F, 0)
    }
}
