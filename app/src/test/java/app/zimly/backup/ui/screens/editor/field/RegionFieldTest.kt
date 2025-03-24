package app.zimly.backup.ui.screens.editor.field

import androidx.compose.ui.focus.FocusState
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert
import org.junit.Test

class RegionFieldTest {

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
