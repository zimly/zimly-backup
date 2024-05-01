package io.zeitmaschine.zimzync.data.s3

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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

    private val progressFlow = MutableStateFlow(0L)
    // https://stackoverflow.com/questions/70935356/how-can-i-calculate-min-max-average-of-continuous-flow-in-kotlin
    // https://docs.oracle.com/javase/8/docs/api/java/util/DoubleSummaryStatistics.html

    fun stepBy(read: Int) {
        progressFlow.tryEmit(read.toLong())
    }

    fun stepTo(skipped: Long) {
        progressFlow.tryEmit(skipped)
    }

    /**
     * Returns kB/s
     */
/*
    fun currentSpeed(): Float {
        if (completed > -1) {
            return 0F
        }

        var duration = lastTick - started
        duration = if (duration > 0) duration else 1 // quard against infinity
        println(duration)
        return readBytes.toFloat() / duration
    }
*/

    /**
     * Returns kB/s
     */
    /*fun avgSpeed(): Float {
        var duration = lastTick - completed
        duration = if (duration > 0) duration else 1
        println(duration)
        return readBytes.toFloat() / duration
    }*/

    fun observe(): Flow<ObjectProgress> {
        return progressFlow
            .takeWhile { it != -1L }
            .map { StreamProgress(it, size) }
            .runningFold(ObjectProgress.EMPTY){ acc, value ->
                val totalBytes = acc.readBytes + value.readBytes
                val percentage = totalBytes.toFloat() / acc.size
                ObjectProgress(readBytes = value.readBytes, totalBytes, percentage = percentage, acc.size)
            }
            // TODO debounce?
    }
}

data class StreamProgress(val readBytes: Long, val size: Long)
data class ObjectProgress(val readBytes: Long, val totalReadBytes: Long, val percentage: Float, val size: Long) {
    companion object {
        val EMPTY = ObjectProgress(0, 0, 0F, 0)
    }
}
