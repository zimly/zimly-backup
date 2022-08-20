package io.zeitmaschine.zimzync

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
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
                        RemoteScreen(LocalContext.current.remoteDataStore)
                    })
            }
        }
    }
}

