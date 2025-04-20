package app.zimly.backup.ui.screens.start

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.zimly.backup.data.db.ZimlyDatabase
import app.zimly.backup.data.db.remote.Remote
import app.zimly.backup.data.media.SourceType
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class StartScreenKtTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyDbShowsGetStarted() {

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(
            context, ZimlyDatabase::class.java
        ).build()

        val dao = db.remoteDao()

        composeTestRule.setContent {
            StartScreen(viewModel = StartViewModel(dao), {}, {})
        }

        composeTestRule.onNodeWithText("Tap the + button below to create your first backup configuration.").assertIsDisplayed()
    }
    @Test
    fun nonEmptyDbShowsList() {

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(
            context, ZimlyDatabase::class.java
        ).build()

        val dao = db.remoteDao()

        runBlocking {
            dao.insert(
                Remote(
                    null,
                    "Test 1",
                    "https://zimly.cloud",
                    "key",
                    "secret",
                    "bucket",
                    null,
                    SourceType.MEDIA,
                    "Pictures"
                )
            )
        }


        composeTestRule.setContent {
            StartScreen(viewModel = StartViewModel(dao), {}, {})
        }

        composeTestRule.onNodeWithText("https://zimly.cloud").assertIsDisplayed()
    }
}