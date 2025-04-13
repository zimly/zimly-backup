package app.zimly.backup.ui.screens.editor.form

import android.net.Uri
import app.zimly.backup.data.media.SourceType
import app.zimly.backup.ui.screens.editor.form.field.BackupSourceField
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

class BackupSourceFieldTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun validInput() = runTest {

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun invalidInput() = runTest {

        val field = BackupSourceField()

        val validations = mutableListOf<Boolean>()
        val errors = mutableListOf<String?>()

        val jobValid = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.valid().take(2).toList(validations)
        }

        val jobError = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.error().take(3).toList(errors)
        }

        field.update(SourceType.FOLDER)
        field.folderField.touch()
        // None selected, folder picker closed
        field.folderField.update(Uri.EMPTY)

        assertFalse("Field should be invalid first", validations.first())
        assertFalse("Field should be invalid after update", validations[1])


        assertNull("No error initially", errors[0])
        assertNull("No error after.. ?", errors[1])
        assertEquals("Select a folder for backup", errors[2])

        jobValid.cancel()
        jobError.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun validateEnforcesErrors() = runTest {

        val field = BackupSourceField()

        val validations = mutableListOf<Boolean>()
        val errors = mutableListOf<String?>()

        val jobValid = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.valid().take(2).toList(validations)
        }

        val jobError = launch(UnconfinedTestDispatcher(testScheduler)) {
            field.error().take(2).toList(errors)
        }

        field.validate()

        assertFalse("Field should be invalid first", validations.first())
        assertFalse("Field should be invalid after validation", validations[1])

        assertNull("No error initially", errors[0])
        assertEquals("Select a collection for backup", errors[1])

        jobValid.cancel()
        jobError.cancel()
    }
}