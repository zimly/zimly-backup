package app.zimly.backup.ui.screens.editor.field

import androidx.compose.ui.focus.FocusState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert
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


    @Test
    fun validRegion() {
        // GIVEN
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val field = RegionField()

        // WHEN
        field.focus(focus)
        field.update("eu-central-1")
        field.focus(focus)

        // THEN
        assert(field.isValid()) { "Valid region, should pass!" }
        MatcherAssert.assertThat(field.state.value.error, `is`(nullValue()))
    }

    @Test
    fun invalidRegion() {
        // GIVEN
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val field = RegionField()

        // WHEN
        field.focus(focus)
        field.update("!invalid")
        field.focus(focus)

        // THEN
        assert(!field.isValid()) { "Not a valid region, should fail!" }
        MatcherAssert.assertThat(field.state.value.error, `is`("Not a valid region."))
    }

    @Test
    fun nullRegionValid() {
        // GIVEN
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val field = RegionField()

        // WHEN
        field.focus(focus)
        field.focus(focus)

        // THEN
        assert(field.isValid()) { "null is a valid region, should pass" }
        MatcherAssert.assertThat(field.state.value.value, `is`(nullValue()))
        MatcherAssert.assertThat(field.state.value.error, `is`(nullValue()))
    }
}
