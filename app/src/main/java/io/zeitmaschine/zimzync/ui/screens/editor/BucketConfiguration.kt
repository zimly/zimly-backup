package io.zeitmaschine.zimzync.ui.screens.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.zeitmaschine.zimzync.ui.theme.containerBackground

@Composable
fun BucketConfiguration(
    name: EditorViewModel.Field,
    url: EditorViewModel.Field,
    key: EditorViewModel.Field,
    secret: EditorViewModel.Field,
    bucket: EditorViewModel.Field
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Outlined.CloudUpload,
                "Media",
                modifier = Modifier.padding(top = 8.dp, end = 8.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            val nameState = name.state.collectAsState()
            val urlState = url.state.collectAsState()
            val keyState = key.state.collectAsState()
            val secretState = secret.state.collectAsState()
            val bucketState = bucket.state.collectAsState()

            OutlinedTextField(
                modifier = Modifier
                    .onFocusChanged { name.focus(it) }
                    .fillMaxWidth(),
                label = { Text("Name") },
                value = nameState.value.value,
                onValueChange = { name.update(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = nameState.value.error != null,
                supportingText = { nameState.value.error?.let { Text(it) } }
            )
            OutlinedTextField(
                modifier = Modifier
                    .onFocusChanged { url.focus(it) }
                    .fillMaxWidth(),
                label = { Text("URL") },
                value = urlState.value.value,
                onValueChange = { url.update(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = urlState.value.error != null,
                supportingText = { urlState.value.error?.let { Text(it) } }
            )
            OutlinedTextField(
                modifier = Modifier
                    .onFocusChanged { key.focus(it) }
                    .fillMaxWidth(),
                label = { Text("Key") },
                value = keyState.value.value,
                onValueChange = { key.update(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = keyState.value.error != null,
                supportingText = { keyState.value.error?.let { Text(it) } }
            )
            OutlinedTextField(
                modifier = Modifier
                    .onFocusChanged { secret.focus(it) }
                    .fillMaxWidth(),
                label = { Text("Secret") },
                value = secretState.value.value,
                onValueChange = { secret.update(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                isError = secretState.value.error != null,
                supportingText = { secretState.value.error?.let { Text(it) } }
            )
            OutlinedTextField(
                modifier = Modifier
                    .onFocusChanged { bucket.focus(it) }
                    .fillMaxWidth(),
                label = { Text("Bucket") },
                value = bucketState.value.value,
                onValueChange = { bucket.update(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = bucketState.value.error != null,
                supportingText = { bucketState.value.error?.let { Text(it) } }
            )
        }
    }
}