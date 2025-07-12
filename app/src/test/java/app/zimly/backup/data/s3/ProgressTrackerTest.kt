package app.zimly.backup.data.s3

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.Test

class ProgressTrackerTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun name() = runTest {
        val totalSize = 5000L
        val tracker = ProgressTracker(totalSize)

        val res = mutableListOf<Progress>()

        // https://developer.android.com/kotlin/flow/test
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            tracker.observe()
                .onEach { println("$it") }
                .toList(res)
        }

        tracker.stepBy(1000L)
        tracker.stepBy(1000L)
        tracker.stepBy(1000L)
        tracker.stepBy(1000L)
        tracker.stepBy(1000L)

        MatcherAssert.assertThat(res.last().totalReadBytes, `is`(totalSize))
        MatcherAssert.assertThat(res.last().percentage, `is`(1F))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun emptySize() = runTest {
        val totalSize = 0L
        val tracker = ProgressTracker(totalSize)

        val res = mutableListOf<Progress>()

        // https://developer.android.com/kotlin/flow/test
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            tracker.observe()
                .onEach { println("$it") }
                .toList(res)
        }

        tracker.stepBy(0L)

        MatcherAssert.assertThat(res.last().totalReadBytes, `is`(totalSize))
        MatcherAssert.assertThat(res.last().percentage, `is`(1F))
    }

}
