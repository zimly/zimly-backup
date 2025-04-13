package app.zimly.backup.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.zimly.backup.ui.screens.editor.form.BucketForm
import app.zimly.backup.ui.theme.ZimzyncTheme
import app.zimly.backup.ui.theme.containerBackground

@Composable
fun BucketConfiguration(
    bucketForm: BucketForm,
    verify: () -> Unit
) {
    val valid by bucketForm.valid().collectAsStateWithLifecycle(false)
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
            Column {
                // Consolidate into one StateFlow?
                val nameState = bucketForm.name.state.collectAsState()
                val urlState = bucketForm.url.state.collectAsState()
                val keyState = bucketForm.key.state.collectAsState()
                val secretState = bucketForm.secret.state.collectAsState()
                val bucketState = bucketForm.bucket.state.collectAsState()
                val regionState = bucketForm.region.state.collectAsState()

                OutlinedTextField(
                    modifier = Modifier
                        .onFocusChanged { bucketForm.name.focus(it) }
                        .fillMaxWidth(),
                    label = { Text("Name") },
                    value = nameState.value.value,
                    onValueChange = { bucketForm.name.update(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = nameState.value.error != null,
                    supportingText = { nameState.value.error?.let { Text(it) } }
                )
                OutlinedTextField(
                    modifier = Modifier
                        .onFocusChanged { bucketForm.url.focus(it) }
                        .fillMaxWidth(),
                    label = { Text("URL") },
                    value = urlState.value.value,
                    onValueChange = { bucketForm.url.update(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = urlState.value.error != null,
                    supportingText = { urlState.value.error?.let { Text(it) } }
                )
                OutlinedTextField(
                    modifier = Modifier
                        .onFocusChanged { bucketForm.region.focus(it) }
                        .fillMaxWidth(),
                    label = { Text("Region (optional)") },
                    // Handle null case, should this go into field instead? value vs state representation.
                    value = regionState.value.value ?: "",
                    onValueChange = { if (it.isEmpty()) bucketForm.region.update(null) else bucketForm.region.update(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = regionState.value.error != null,
                    supportingText = { regionState.value.error?.let { Text(it) } }
                )
                OutlinedTextField(
                    modifier = Modifier
                        .onFocusChanged { bucketForm.key.focus(it) }
                        .fillMaxWidth(),
                    label = { Text("Key") },
                    value = keyState.value.value,
                    onValueChange = { bucketForm.key.update(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = keyState.value.error != null,
                    supportingText = { keyState.value.error?.let { Text(it) } }
                )
                OutlinedTextField(
                    modifier = Modifier
                        .onFocusChanged { bucketForm.secret.focus(it) }
                        .fillMaxWidth(),
                    label = { Text("Secret") },
                    value = secretState.value.value,
                    onValueChange = { bucketForm.secret.update(it) },
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
                        .onFocusChanged { bucketForm.bucket.focus(it) }
                        .fillMaxWidth(),
                    label = { Text("Bucket") },
                    value = bucketState.value.value,
                    onValueChange = { bucketForm.bucket.update(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = bucketState.value.error != null,
                    supportingText = { bucketState.value.error?.let { Text(it) } }
                )
            }
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center, // Arrangement.Absolute.Right
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    enabled = valid,
                    onClick = verify,
                    // Padding the icon size conditionally
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .height(32.dp),
                ) {
                    Text(text = "Verify")
                }
            }
        }
    }
}

@Preview
@Composable
fun BucketConfigurationPreview() {
    val bucketForm = BucketForm()
    ZimzyncTheme {
        BucketConfiguration(bucketForm) {}
    }
}