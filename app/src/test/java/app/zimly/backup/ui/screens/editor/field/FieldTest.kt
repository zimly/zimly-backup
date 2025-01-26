package app.zimly.backup.ui.screens.editor.field

import androidx.compose.ui.focus.FocusState
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class FieldTest {

    @Test
    fun errorAfterInvalidUpdate() {
        // GIVEN
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val errorMessage = "Fail!"
        val validator: (value: String) -> Boolean = { false }
        val field = Field(errorMessage, validator)

        // WHEN
        field.focus(focus)
        field.update("whatever")
        field.focus(focus)

        // THEN
        assertThat(field.state.value.error, `is`(errorMessage))
    }


    @Test
    fun errorAfterFocusLost() {
        // GIVEN
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val errorMessage = "No touchy!"
        val field = Field(errorMessage)

        // WHEN
        field.focus(focus)

        // THEN
        assertThat(field.state.value.error, nullValue())

        // WHEN
        field.focus(focus)
        // THEN
        assertThat(field.state.value.error, `is`(errorMessage))
    }
}