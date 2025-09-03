package app.zimly.backup.ui.screens.sync.permission

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class DocumentsPermissionContainerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun warningToggles() {

        val showWarningState = MutableStateFlow(true)

        composeTestRule.setContent {
            DocumentsPermissionContainer(
                edit = {showWarningState.value = false},
                folderPath = "",
                writePermission = true,
                viewModel = object : DocumentsPermissionVMContract {
                    override val showWarning: StateFlow<Boolean>
                        get() {
                            return showWarningState
                        }
                }
            )
        }

        composeTestRule.onNodeWithText("Missing Folder Permissions")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("update-button").performClick()

        composeTestRule.onNodeWithText("Missing Folder Permissions")
            .assertIsNotDisplayed()
    }
}