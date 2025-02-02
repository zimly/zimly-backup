package app.zimly.backup.ui.screens.crash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CrashViewModel(throwable: Throwable?) : ViewModel() {

    private val _error = MutableStateFlow(ErrorState(throwable?.message ?: "Unknown Error!", throwable.toString()))
    val errorState: StateFlow<ErrorState> = _error.asStateFlow()

    data class ErrorState(
        val message: String,
        val stack: String
    )
}

@Composable
fun CrashScreen(
    throwable: Throwable?,
    viewModel: CrashViewModel = viewModel(factory = viewModelFactory {
        initializer {
            CrashViewModel(throwable)
        }
    })
) {

    val state by viewModel.errorState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "An error occurred:", color = Color.Red, fontSize = 20.sp)
        Text(
            text = state.message,
            color = Color.Black,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = state.stack,
            color = Color.Black,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )


        Button(onClick = { }) {
            Text("Go Back")
        }
    }
}
