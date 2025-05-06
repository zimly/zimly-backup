package app.zimly.backup.ui.screens.start

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.zimly.backup.data.db.ZimlyDatabase
import app.zimly.backup.data.db.remote.Remote
import app.zimly.backup.data.db.remote.SyncDirection
import app.zimly.backup.data.media.ContentType
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
            StartScreen(viewModel = StartViewModel(dao), { _, _ -> }, {})
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
                    ContentType.MEDIA,
                    "Pictures",
                    SyncDirection.UPLOAD
                )
            )
        }


        composeTestRule.setContent {
            StartScreen(viewModel = StartViewModel(dao), { _, _ -> }, {})
        }

        composeTestRule.onNodeWithText("https://zimly.cloud").assertIsDisplayed()
    }

    @Test
    fun copyConfiguration() {

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
                    ContentType.MEDIA,
                    "Pictures",
                    SyncDirection.UPLOAD
                )
            )
        }


        composeTestRule.setContent {
            StartScreen(viewModel = StartViewModel(dao), { _,_ -> }, {})
        }

        composeTestRule.onNodeWithTag("Zimly Title").assertIsDisplayed()

        composeTestRule.onNodeWithText("Test 1").performTouchInput {
            longClick()
        }

        composeTestRule.onNodeWithTag("Zimly Title").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("List Selection Actions").assertIsDisplayed()

        composeTestRule.onNodeWithText("1 selected").assertIsDisplayed()

        composeTestRule.onNodeWithTag("Copy Selected").performClick()

        composeTestRule.onNodeWithText("Test 1 (Copy)").assertIsDisplayed()
    }

}