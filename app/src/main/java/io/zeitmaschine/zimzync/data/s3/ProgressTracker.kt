package io.zeitmaschine.zimzync.data.s3

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.takeWhile
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource


class ProgressTracker(private val size: Long) {

    private val timeSource = TimeSource.Monotonic

    // TODO the markNow() is too close to the first emit. Putting it on the Response should help?
    private val start = Progress(0, 0, 0F, size, 0, timeSource.markNow())

    // Important: MutableStateFlow does not emit same values!
    private val progressFlow = MutableSharedFlow<Pair<Long, ComparableTimeMark>>(500)
    // https://stackoverflow.com/questions/70935356/how-can-i-calculate-min-max-average-of-continuous-flow-in-kotlin
    // https://docs.oracle.com/javase/8/docs/api/java/util/DoubleSummaryStatistics.html

    fun stepBy(read: Long) {
        progressFlow.tryEmit(Pair(read, timeSource.markNow()))
    }

    fun observe(): Flow<Progress> {
        return progressFlow
            .runningFold(start) { acc, pro ->
                val readBytes = pro.first
                val timeMark = pro.second
                val totalBytes = acc.totalReadBytes + readBytes
                val percentage = totalBytes.toFloat() / size

                var duration = (timeMark - acc.timeMark).inWholeMilliseconds

                // Divide by zero safe guard
                duration = if (duration > 0) duration else 1

                // Bytes/µs to Bytes/s
                val bytesPerSec = readBytes * 1000 / duration
                Progress(readBytes, totalBytes, percentage, size, bytesPerSec, timeMark)
            }.takeWhile { it.percentage <= 1.0 }

    }
}

data class Progress(
    val readBytes: Long,
    val totalReadBytes: Long,
    val percentage: Float,
    val size: Long,
    val bytesPerSec: Long,
    val timeMark: ComparableTimeMark
)