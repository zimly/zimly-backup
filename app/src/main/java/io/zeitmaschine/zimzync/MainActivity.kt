package io.zeitmaschine.zimzync

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZimzyncTheme {
                // A surface container using the 'background' color from the theme
                Scaffold(
                    floatingActionButtonPosition = FabPosition.End,
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            startActivity(Intent(this, EditActivity::class.java))
                        }) {
                            Icon(Icons.Filled.Add, "Add Remote")
                        }
                    },
                    content = {
                        RemoteScreen()
                    })
            }
        }
    }
}
data class MainUiState(
    val remotes: List<Remote> = listOf(
        Remote("test1", "s1.zeitmaschine.io", "zm"),
        Remote("test2", "s2.zeitmaschine.io", "zm"),
        Remote("test2", "s2.zeitmaschine.io", "zm"),
        Remote("test2", "s2.zeitmaschine.io", "zm"),
        Remote("test2", "s2.zeitmaschine.io", "zm"),
        Remote("test2", "s2.zeitmaschine.io", "zm"),
        Remote("test2", "s2.zeitmaschine.io", "zm"),
        Remote("test2", "s2.zeitmaschine.io", "zm"),
        Remote("test2", "s2.zeitmaschine.io", "zm"),
        Remote("test2", "s2.zeitmaschine.io", "zm"),
        Remote("test2", "s2.zeitmaschine.io", "zm"),
        Remote("test2", "s2.zeitmaschine.io", "zm"),
        Remote("test3", "s3.zeitmaschine.io", "zm"),
        Remote("test4", "s4.zeitmaschine.io", "zm"),
        Remote("test5", "s5.zeitmaschine.io", "zm")
    )
)

class MainViewModel: ViewModel() {
    var uiState by mutableStateOf(MainUiState())
}

@Composable
fun RemoteScreen(viewModel: MainViewModel = MainViewModel()) {
    val uiState = viewModel.uiState
    RemoteComponent(remotes = uiState.remotes)
}

@Composable
fun RemoteComponent(remotes: List<Remote>) {

    val current = LocalContext.current
    LazyColumn {
        items(remotes) { remote ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clickable(onClick = {
                        current.startActivity(Intent(current, EditActivity::class.java))
                    })
            ) {
                Column {
                    Text(remote.name)
                    Text(remote.url)
                }
                if (remote.lastSynced != null)
                    Text(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(remote.lastSynced?.toInstant()))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ZimzyncTheme {
        val remotes = emptyList<Remote>()
        RemoteComponent(remotes)
    }
}