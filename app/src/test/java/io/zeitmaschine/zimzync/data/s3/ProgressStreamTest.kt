package io.zeitmaschine.zimzync.data.s3

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.nio.charset.StandardCharsets


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
}