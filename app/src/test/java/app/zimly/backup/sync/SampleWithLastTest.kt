package app.zimly.backup.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SampleWithLastTest {

    @Test
    fun `sampleWithLast should emit periodically and include final value`() = runTest {
        val values = listOf(1, 2, 3, 4, 5)
        val emitted = mutableListOf<Int>()

        val testFlow = flow {
            for (value in values) {
                emit(value)
                advanceTimeBy(100) // simulate 100ms between emissions
            }
        }

        val result = testFlow
            .sampleWithLast(periodMillis = 250)
            .toList(emitted)

        // Simulate end of flow
        advanceUntilIdle()

        // Sample should emit around 250ms (after 3), and again at 500ms (after 5), then flush final
        assertEquals(listOf(3, 5), result)
    }

}