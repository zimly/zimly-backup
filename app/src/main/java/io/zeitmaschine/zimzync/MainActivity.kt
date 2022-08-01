package io.zeitmaschine.zimzync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZimzyncTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RemoteList(emptyList())
                }
            }
        }
    }
}

@Composable
fun RemoteList(remotes: List<Remote>) {
    Column {
        remotes.forEach { remote ->
            Text(remote.name)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ZimzyncTheme {
        RemoteList(listOf(Remote("test"), Remote("test2")))
    }
}