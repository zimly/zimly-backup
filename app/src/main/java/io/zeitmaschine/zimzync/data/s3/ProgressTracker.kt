package io.zeitmaschine.zimzync.data.s3

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.takeWhile
import java.io.FilterInputStream
import java.io.InputStream
import kotlin.time.TimeSource


class ProgressTracker(private val size: Long) {

    private val timeSource = TimeSource.Monotonic

    // Important: MutableStateFlow does not emit same values!
    private val progressFlow = MutableSharedFlow<Long>(500)
    // https://stackoverflow.com/questions/70935356/how-can-i-calculate-min-max-average-of-continuous-flow-in-kotlin
    // https://docs.oracle.com/javase/8/docs/api/java/util/DoubleSummaryStatistics.html

    fun stepBy(read: Int) {
        progressFlow.tryEmit(read.toLong())
    }

    fun stepTo(skipped: Long) {
        progressFlow.tryEmit(skipped)
    }

    fun observe(): Flow<Progress> {
        return progressFlow
            .takeWhile { it != -1L }
            .runningFold(Progress.EMPTY){ acc, inc ->
                val totalBytes = acc.totalReadBytes + inc
                val percentage = totalBytes.toFloat() / size

                val timeMark = timeSource.markNow()
                var duration = (timeMark - acc.timeMark).inWholeMicroseconds

                // Divide by zero safe guard,
                duration = if (duration > 0) duration else 1

                // Bytes/µs to Bytes/s
                val bytesPerSec = inc * 1000_000 / duration
                Progress(inc, totalBytes, percentage, size, bytesPerSec, timeMark)
            }
    }
}

data class Progress(val readBytes: Long, val totalReadBytes: Long, val percentage: Float, val size: Long, val bytesPerSec: Long, val timeMark: TimeSource.Monotonic.ValueTimeMark) {
    companion object {
        val EMPTY = Progress(0, 0, 0F, 0, 0, TimeSource.Monotonic.markNow())
    }
}

