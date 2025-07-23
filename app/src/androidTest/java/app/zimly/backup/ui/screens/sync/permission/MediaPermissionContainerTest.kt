package app.zimly.backup.ui.screens.sync.permission

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.zimly.backup.permission.MediaPermissionService
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class MediaPermissionContainerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionWarningAndDialog() {

        val permissionService = mockk<MediaPermissionService>()

        every { permissionService.permissionsGranted() } returns false
        every { permissionService.requiredPermissions() } returns arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )

        every { permissionService.permissionsDenied(any()) } returns false

        // Start the app
        composeTestRule.setContent {
            MediaPermissionContainer(viewModel = MediaPermissionViewModel(permissionService))
        }

        // helper
        fun permissionDialog() = hasTestTag("permissions_dialog")

        // Warning displayed
        composeTestRule.onNodeWithText("Missing Media Permissions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Learn More").performClick()

        // Dialog with actions opened
        composeTestRule.onNode(permissionDialog()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Permissions Required").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant Permissions").assertIsDisplayed()

        // Close dialog without action
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Dialog is gone
        composeTestRule.onNode(permissionDialog()).assertIsNotDisplayed()

    }
}