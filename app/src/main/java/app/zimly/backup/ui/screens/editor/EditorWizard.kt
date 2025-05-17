package app.zimly.backup.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.zimly.backup.ui.components.NotificationBar
import app.zimly.backup.ui.components.NotificationProvider


/**
 * Provides the chrome for the individual wizard steps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardStep(
    title: String,
    notificationProvider: NotificationProvider? = null,
    navigation: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val snackbarState = remember { SnackbarHostState() }

    notificationProvider?.let {
        NotificationBar(it, snackbarState)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.SpaceBetween
            ) {
                navigation()
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }
    ) { innerPadding ->

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

