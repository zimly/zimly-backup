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
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextFieldTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun validInput() = runTest {
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val errorMessage = "Fail!"
        val validInput = "test"
        val validator: (value: String) -> Boolean = { it == validInput }
        val field = TextField(errorMessage, validator)

        val validations = mutableListOf<Boolean>()
        val errors = mutableListOf<String?>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.valid().take(2).toList(validations)
            field.error().take(2).toList(errors)
        }

        field.update(validInput)

        assertFalse("Field should be invalid first", validations.first())
        assertTrue("Field should be valid after update", validations.last())

        errors.forEach { assertNull("No error message expected", it) }

        job.cancel()
    }

    @Test
    fun errorAfterInvalidUpdate() {
        // GIVEN
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val errorMessage = "Fail!"
        val validator: (value: String) -> Boolean = { false }
        val field = TextField(errorMessage, validator)

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
        val field = TextField(errorMessage)

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