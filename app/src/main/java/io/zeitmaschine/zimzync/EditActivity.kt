package io.zeitmaschine.zimzync

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme

class EditActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZimzyncTheme {
                // A surface container using the 'background' color from the theme
                Scaffold(
                    content = {
                        EditRemote(remote = Remote("", "", ""))
                    },
                )
            }
        }
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditRemote(remote: Remote) {
    val current = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(all = 16.dp)
        ) {

        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL") },
            value = remote.url,
            onValueChange = { value -> remote.url = value },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Key") },
            value = remote.key,
            onValueChange = { value -> remote.key = value },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Secret") },
            value = remote.secret,
            onValueChange = { value -> remote.secret = value },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = {
            Toast
                .makeText(current, "OnClick", Toast.LENGTH_LONG)
                .show()
        })
        {
            Text(text = "Save")
        }
    }


}


@Preview(showBackground = true)
@Composable
fun EditPreview() {
    ZimzyncTheme {
        EditRemote(Remote("", "", ""))
    }
}