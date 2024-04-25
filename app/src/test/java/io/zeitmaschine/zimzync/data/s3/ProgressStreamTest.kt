package io.zeitmaschine.zimzync.data.s3

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
import java.io.InputStream
import java.nio.charset.StandardCharsets


class ProgressStreamTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun name() = runTest {
        val initialString = "textavdfbad"
        val input: InputStream = ByteArrayInputStream(initialString.toByteArray())

        val size = initialString.toByteArray().size.toLong()

        val progress = ProgressTracker(size)
        val wrapped = ProgressStream.wrap(input, progress)

        // https://developer.android.com/kotlin/flow/test
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {

            val lastProgress: Progress = progress.observe()
                .onEach { println("$it") }
                .last()

            assertThat(lastProgress.readBytes, `is`(size))
            assertThat(lastProgress.percentage, `is`(1F))
        }

        String(wrapped.readAllBytes(), StandardCharsets.UTF_8)
    }
}