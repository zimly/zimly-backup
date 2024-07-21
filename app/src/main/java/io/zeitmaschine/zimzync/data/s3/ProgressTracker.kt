package io.zeitmaschine.zimzync.data.s3

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.transformWhile
import kotlin.time.TimeSource


class ProgressTracker(private val size: Long, private val chunkSize: Int = 5) {

    private val timeSource = TimeSource.Monotonic
    private var totalRead = 0L

    // Important: MutableStateFlow does not emit same values!
    private val progressFlow = MutableSharedFlow<Long>(500)
    // https://stackoverflow.com/questions/70935356/how-can-i-calculate-min-max-average-of-continuous-flow-in-kotlin
    // https://docs.oracle.com/javase/8/docs/api/java/util/DoubleSummaryStatistics.html

    fun stepBy(read: Int) {
        progressFlow.tryEmit(read.toLong())
    }

    fun observe(): Flow<Progress> {
        return progressFlow
            .transformWhile {
                emit(it)
                totalRead += it
                totalRead < size
            }
            .chunked(chunkSize) // normalizing over chunks for better speed calculation
            .runningFold(Progress.EMPTY) { acc, chunk ->
                val readBytes = chunk.sum()
                val totalBytes = acc.totalReadBytes + readBytes
                val percentage = totalBytes.toFloat() / size

                val timeMark = timeSource.markNow()
                var duration = (timeMark - acc.timeMark).inWholeMicroseconds

                // Divide by zero safe guard
                duration = if (duration > 0) duration else 1

                // Bytes/µs to Bytes/s
                val bytesPerSec = readBytes * 1000_000 / duration
                Progress(readBytes, totalBytes, percentage, size, bytesPerSec, timeMark)
            }

    }
}

data class Progress(
    val readBytes: Long,
    val totalReadBytes: Long,
    val percentage: Float,
    val size: Long,
    val bytesPerSec: Long,
    val timeMark: TimeSource.Monotonic.ValueTimeMark
) {
    companion object {
        val EMPTY = Progress(0, 0, 0F, 0, 0, TimeSource.Monotonic.markNow())
    }
}

/**
 * Taken from https://github.com/Kotlin/kotlinx.coroutines/pull/4127/files
 *
 * Splits the given flow into a flow of non-overlapping lists each not exceeding the given [size] but never empty.
 * The last emitted list may have fewer elements than the given size.
 *
 * Example of usage:
 * ```
 * flowOf("a", "b", "c", "d", "e")
 *     .chunked(2) // ["a", "b"], ["c", "d"], ["e"]
 *     .map { it.joinToString(separator = "") }
 *     .collect {
 *         println(it) // Prints "ab", "cd", e"
 *     }
 * ```
 *
 * @throws IllegalArgumentException if [size] is not positive.
 */
fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> {
    require(size >= 1) { "Expected positive chunk size, but got $size" }
    return flow {
        var result: ArrayList<T>? = null // Do not preallocate anything
        collect { value ->
            // Allocate if needed
            val acc = result ?: ArrayList<T>(size).also { result = it }
            acc.add(value)
            if (acc.size == size) {
                emit(acc)
                // Cleanup, but don't allocate -- it might've been the case this is the last element
                result = null
            }
        }
        result?.let { emit(it) }
    }
}

