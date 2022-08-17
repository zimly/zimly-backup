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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme

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
                        RemoteScreen(LocalContext.current.testDataStore)
                    })
            }
        }
    }
}

class MainViewModel(dataStore: DataStore<Test>) : ViewModel() {
    val remotes = dataStore.data
}


@Composable
fun RemoteScreen(
    dataStore: DataStore<Test>,
    // https://programmer.ink/think/a-new-way-to-create-a-viewmodel-creationextras.html
    viewModel: MainViewModel = viewModel(factory = viewModelFactory {
        initializer {
            MainViewModel(dataStore)
        }
    })
) {
    val remote = viewModel.remotes.collectAsState(initial = test {
        name = "hawrefups"
        url = "test.com"
        key = "test.com"
        secret = "test.com"
        date = 123456
    }).value
    RemoteComponent(remotes = listOf(remote))
}

@Composable
fun RemoteComponent(remotes: List<Test>) {

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
                if (remote.date != null) {
                    Text(remote.date.toString())
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ZimzyncTheme {
        val remotes = emptyList<Test>()
        RemoteComponent(remotes)
    }
}