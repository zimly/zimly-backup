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
import org.junit.Assert.assertEquals
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

        field.focus(focus)
        field.update(validInput)
        field.focus(focus)

        assertFalse("Field should be invalid first", validations.first())
        assertTrue("Field should be valid after update", validations.last())

        errors.forEach { assertNull("No error message expected", it) }

        job.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun errorAfterInvalidUpdate() = runTest {
        // GIVEN
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val errorMessage = "Fail!"
        val validator: (value: String) -> Boolean = { false }
        val field = TextField(errorMessage, validator)

        val validations = mutableListOf<Boolean>()
        val errors = mutableListOf<String?>()

        val jobValid = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.valid().take(2).toList(validations)
        }

        val jobError = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.error().take(3).toList(errors)
        }

        field.focus(focus)
        field.update("whatever")
        field.focus(focus)

        assertFalse("Field should be invalid first", validations.first())
        assertFalse("Field should be invalid after update", validations.last())

        // initial null value not emitted?
        assertNull("No error initially", errors[0])
        assertNull("No error after update", errors[1])
        assertEquals("Error after focus loss", errorMessage, errors[2])

        jobValid.cancel()
        jobError.cancel()
    }
}