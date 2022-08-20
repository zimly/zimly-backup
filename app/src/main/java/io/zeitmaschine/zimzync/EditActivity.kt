package io.zeitmaschine.zimzync

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.LocalContext
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme

class EditActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZimzyncTheme {
                val current = LocalContext.current

                // A surface container using the 'background' color from the theme
                Scaffold(
                        content = {
                        EditRemote(remote = remote {
                            name = ""
                            url = ""
                            key = ""
                            secret = ""
                        }, saveEntry = { current.startActivity(Intent(current, MainActivity::class.java)) })
                    },
                )
            }
        }
    }
}
