package app.zimly.backup.ui.screens.editor.field

import androidx.compose.ui.focus.FocusState
import app.zimly.backup.data.media.SourceType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class BackupSourceFieldTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun validInput() = runTest {
        val focus = mockk<FocusState>()
        every { focus.hasFocus } returns true andThen false

        val validInput = "test-collection"

        val field = BackupSourceField()

        val validations = mutableListOf<Boolean>()
        val errors = mutableListOf<String?>()

        val jobValid = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.valid().take(2).toList(validations)
        }

        val jobError = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.error().take(3).toList(errors)
        }

        field.update(SourceType.MEDIA)
        field.mediaField.update(validInput)

        assertFalse("Field should be invalid first", validations.first())
        assertTrue("Field should be valid after update", validations.last())

        errors.forEach { assertNull("No error message expected", it) }

        jobValid.cancel()
        jobError.cancel()
    }

}