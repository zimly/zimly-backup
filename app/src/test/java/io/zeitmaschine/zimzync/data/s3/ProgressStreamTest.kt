package io.zeitmaschine.zimzync.data.s3

import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Objects
import kotlin.concurrent.Volatile
import kotlin.random.Random


class ProgressStreamTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun name() = runTest {
        val image = "/testdata/test_image.png"
        val stream =
            javaClass.getResourceAsStream(image) ?: throw Error("Could not open test resource.")
        val size = stream.available().toLong()

        val progress = ProgressTracker(size)
        val wrapped = ProgressStream.wrap(stream, progress)

        // https://developer.android.com/kotlin/flow/test
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {

            val lastProgress: Progress = progress.observe()
                .onEach { println("$it") }
                .last()

            assertThat(lastProgress.totalReadBytes, `is`(size))
            assertThat(lastProgress.percentage, `is`(1F))
            // TODO not a very smart assertion, can be 0
            assertThat(lastProgress.bytesPerSec > 0, `is`(true))
        }

        String(wrapped.readAllBytes(), StandardCharsets.UTF_8)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun slowDownstream() = runTest {
        val size = 1000
        val chunkSize = 100
        val progress = ProgressTracker(size.toLong())

        // https://developer.android.com/kotlin/flow/test
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {

            val res = mutableListOf<Progress>()
            progress.observe()
                .onEach { println("$it") }
                .toList(res)

            // First 2 entries are 0
            res.subList(0, 1).forEach {
                assertTrue(it.bytesPerSec == 0L)
            }

            // Rest is around ~200 B/s
            res.subList(2, res.size).forEach {
                assertTrue(it.bytesPerSec in 190..210)
            }
            // Totals add up
            assertThat(res.last().totalReadBytes, `is`(size.toLong()))
            assertThat(res.last().percentage, `is`(1F))
        }

        val rndBytes = Random.Default.nextBytes(size)
        val input = ByteArrayInputStream(rndBytes)
        val out = SlowOutputStream()
        val wrapped = ProgressStream.wrap(input, progress)

        wrapped.use { w ->
            out.use { o ->
                val buf = ByteArray(chunkSize)
                var length: Int
                while ((w.read(buf).also { length = it }) != -1) {
                    o.write(buf, 0, length)
                }
            }
        }
    }

    private class SlowOutputStream(private val timeBuffer: Long = 500) : OutputStream() {
        @Volatile
        private var closed = false

        @Throws(IOException::class)
        private fun ensureOpen() {
            if (closed) {
                throw IOException("Stream closed")
            }
        }

        @Throws(IOException::class)
        override fun write(b: Int) {
            ensureOpen()
            Thread.sleep(timeBuffer)
        }

        @Throws(IOException::class)
        override fun write(b: ByteArray, off: Int, len: Int) {
            Objects.checkFromIndexSize(off, len, b.size)
            ensureOpen()
            println("Writing $b with len $len and off $off")
            Thread.sleep(timeBuffer)
        }

        override fun close() {
            closed = true
        }
    }
}