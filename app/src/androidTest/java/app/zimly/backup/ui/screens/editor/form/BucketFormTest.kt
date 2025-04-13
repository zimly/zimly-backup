package app.zimly.backup.ui.screens.editor.form

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

import org.junit.Test

class BucketFormTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun validate() = runTest {
        val bucketForm = BucketForm()

        val validations = mutableListOf<Boolean>()
        val errors = mutableListOf<List<String>>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            bucketForm.valid().take(2).toList(validations)
            bucketForm.errors().take(5).toList(errors)
        }

        bucketForm.validate()

        assertFalse("Field should be invalid first", validations.first())
        assertFalse("In valid after validate", validations[1])
        assertTrue("Two validation emitted", validations.size == 2)

        // TODO: These are dependant on the order of app.zimly.backup.ui.screens.editor.form.BucketForm.getFields
        //name, url, key, secret, bucket, region
        assertTrue("Name errors", errors.last()[0].contains("This field is required."))
        assertTrue("Bucket URL errors", errors.last()[1].contains("Not a valid URL."))
        assertTrue("Key errors", errors.last()[2].contains("This field is required."))
        assertTrue("Secret errors", errors.last()[3].contains("This field is required."))
        assertFalse("Region is optional, no error", errors.last().contains("Not a valid region."))

        job.cancel()
    }
}