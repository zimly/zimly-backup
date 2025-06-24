package app.zimly.backup.ui.screens.crash

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CrashViewModel(message: String?, stack: String?) : ViewModel() {

    private val _error = MutableStateFlow(ErrorState(message ?: "Unknown Error", stack))
    val errorState: StateFlow<ErrorState> = _error.asStateFlow()

    data class ErrorState(
        val message: String,
        val stack: String?
    )
}

@Composable
fun CrashScreen(
    message: String?,
    stack: String?,
    viewModel: CrashViewModel = viewModel(factory = viewModelFactory {
        initializer {
            CrashViewModel(message, stack)
        }
    })
) {

    val state by viewModel.errorState.collectAsState()
    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.displayCutout),
        bottomBar = {
            Actions()
        }
    ) { innerPadding ->

        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(all = 16.dp) then Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ) then Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            ErrorTitle()
            ErrorDetails(state)
        }
    }

}

@Composable
private fun ErrorTitle() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "Oops!",
            color = MaterialTheme.colorScheme.error,
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
            modifier = Modifier.padding(7.dp)
        )
        Text(
            text = "Something went wrong",
            color = MaterialTheme.colorScheme.error,
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
            modifier = Modifier.padding(7.dp)
        )
        Text(
            "Zimly encountered an unexpected error. Please help us improve by reporting this issue.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(7.dp)
        )
    }
}

@Composable
private fun ErrorDetails(state: CrashViewModel.ErrorState) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Card(
            colors = CardDefaults.cardColors(containerColor = containerBackground()),
        ) {
            Text(
                text = state.message,
                lineHeight = MaterialTheme.typography.titleSmall.lineHeight,
                fontSize = MaterialTheme.typography.titleSmall.fontSize,
                modifier = Modifier.padding(16.dp)
            )
            state.stack?.let {
                Text(
                    lineHeight = 10.sp,
                    text = it,
                    fontSize = 8.sp,
                    modifier = Modifier
                        .padding(16.dp)
                        .height(64.dp)
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState())
                )
            }

            Row(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center // Arrangement.Absolute.Right
            ) {
                TextButton(
                    onClick = { state.stack?.let { clipboardManager.setText(AnnotatedString(it)) } },
                    contentPadding = PaddingValues(horizontal = 24.dp), // Reset padding
                    modifier = Modifier
                        .height(32.dp),
                ) {
                    Text(text = "Copy to Clipboard")
                }
            }
        }

    }
}

@Composable
private fun Actions() {
    val uriHandler = LocalUriHandler.current
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp)
    ) {

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TextButton({ uriHandler.openUri("https://github.com/zimly/zimly-backup/issues") }) {
                Text("Report on Github")
            }
            TextButton({ uriHandler.openUri("mailto:espen.jervidalo@gmail.com") }) {
                Text("Email Support")
            }
        }
    }
}
