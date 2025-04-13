package app.zimly.backup.ui.screens.editor.form

import androidx.compose.ui.focus.FocusState
import app.zimly.backup.ui.screens.editor.form.field.RegionField
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionFieldTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun validUntouched() = runTest {

        val field = RegionField()

        val validations = mutableListOf<Boolean>()
        val errors = mutableListOf<String?>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.valid().take(3).toList(validations)
            field.error().take(3).toList(errors)
        }

        assertTrue("Field should be valid first", validations.first())
        assertTrue("Only one validation emitted", validations.size == 1)
        assertTrue("No errors emitted", errors.isEmpty())

        job.cancel()
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun validRegion() = runTest {
        // GIVEN
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val field = RegionField()

        val validations = mutableListOf<Boolean>()
        val errors = mutableListOf<String?>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.valid().take(3).toList(validations)
            field.error().take(3).toList(errors)
        }

        // WHEN
        field.focus(focus)
        field.update("eu-central-1")
        field.focus(focus)

        // THEN
        validations.forEach { assertTrue("Fields should be valid", it) }
        assertEquals(2, validations.size)
        assertTrue("No errors emitted", errors.isEmpty())

        job.cancel()

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun invalidRegion() = runTest {
        // GIVEN
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val field = RegionField()

        val validations = mutableListOf<Boolean>()
        val errors = mutableListOf<String?>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.valid().take(3).toList(validations)
            field.error().take(3).toList(errors)
        }

        // WHEN
        field.focus(focus)
        field.update("!invalid")
        field.focus(focus)

        // THEN
        assertTrue("Field should be valid first", validations.first())
        assertFalse("Field should be invalid after update", validations[1])
        assertFalse("Field should be invalid after losing focus", validations[2])
        assertTrue("Only 3 emits", validations.size == 3)

        assertEquals("Not a valid region.", errors.first())
        job.cancel()

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun nullRegionValid() = runTest {
        // GIVEN
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val field = RegionField()

        val validations = mutableListOf<Boolean>()
        val errors = mutableListOf<String?>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.valid().take(3).toList(validations)
            field.error().take(3).toList(errors)
        }

        // WHEN
        field.focus(focus)
        field.update(null)
        field.focus(focus)

        job.cancel()
    }

}
