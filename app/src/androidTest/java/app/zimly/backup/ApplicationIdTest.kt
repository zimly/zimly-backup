package app.zimly.backup

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ApplicationIdTest {

    /**
     * After publishing the app to the Play Store the applicationID cannot be changed.
     * This corresponds to the packageName even though they've been decoupled.
     */
    @Test
    fun deprecatedApplicationId() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals("io.zeitmaschine.zimzync", appContext.packageName)
    }
}