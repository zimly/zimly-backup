package app.zimly.backup.ui.screens.editor.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.s3.MinioRepository
import app.zimly.backup.ui.components.Notification
import app.zimly.backup.ui.components.NotificationProvider
import app.zimly.backup.ui.screens.editor.form.BucketForm
import app.zimly.backup.ui.screens.editor.form.field.BaseField
import app.zimly.backup.ui.screens.editor.form.field.FieldState
import app.zimly.backup.ui.screens.editor.steps.components.WizardStep
import app.zimly.backup.ui.theme.ZimzyncTheme
import app.zimly.backup.ui.theme.containerBackground
import io.minio.errors.ErrorResponseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BucketViewModel(
    private val store: ValueStore<BucketForm.BucketConfiguration>,
) : ViewModel(), NotificationProvider {

    val bucketForm = BucketForm()
    val notification: MutableStateFlow<Notification?> = MutableStateFlow(null)

    init {
        viewModelScope.launch {
            store.load()
                .filterNotNull()
                .collectLatest { bucketForm.populate(it) }
        }
    }

    fun persist(nextStep: () -> Unit) {
        store.persist(bucketForm.values()) { success -> if (success) nextStep() }
    }

    suspend fun verify() {
        val bucketConfiguration = bucketForm.values()
        val repo = MinioRepository(
            bucketConfiguration.url,
            bucketConfiguration.key,
            bucketConfiguration.secret,
            bucketConfiguration.bucket,
            bucketConfiguration.region,
            bucketConfiguration.virtualHostedStyle
        )

        try {
            val bucketExists = repo.bucketExists()
            val message =
                if (bucketExists) "Connection successful, bucket exists!" else "Bucket does not exist!"
            notification.update { Notification(message = message, type = Notification.Type.INFO) }
        } catch (e: Exception) {
            val cause = e.cause ?: e
            var message = cause.message
            if (cause is ErrorResponseException) {
                cause.errorResponse().message()
                val status = cause.response().code
                val mes = cause.errorResponse().message()
                val errorCode = cause.errorResponse().code()
                message = "status: $status, message: $mes, errorCode: $errorCode"
            }
            notification.update {
                Notification(
                    message = "Connection failed: $message",
                    type = Notification.Type.ERROR
                )
            }
        }
    }

    override fun reset() {
        notification.update { null }
    }

    override fun get(): StateFlow<Notification?> {
        return notification.asStateFlow()
    }

}

@Composable
fun BucketConfigurationStep(
    store: ValueStore<BucketForm.BucketConfiguration>,
    parentNotificationProvider: NotificationProvider,
    nextStep: () -> Unit,
    previousStep: () -> Unit,
    viewModel: BucketViewModel = viewModel(factory = viewModelFactory {
        initializer {
            BucketViewModel(store)
        }
    })
) {

    val isValid by viewModel.bucketForm.valid().collectAsStateWithLifecycle(false)

    val notProv = parentNotificationProvider.combine(viewModel)

    WizardStep(
        title = "Bucket Configuration",
        notificationProvider = notProv,
        navigation = {
            TextButton(onClick = { previousStep() }) {
                Text("Back")
            }
            TextButton(
                enabled = isValid,
                onClick = {
                    viewModel.persist(nextStep)
                },
            ) {
                Text("Save")
            }
        }) {
        BucketConfiguration(
            bucketForm = viewModel.bucketForm,
            verify = { viewModel.viewModelScope.launch { viewModel.verify() } }
        )
    }
}

@Composable
fun <T> BaseField<T>.rememberTextFieldState(
    toText: (T) -> String,
    fromText: (String) -> T
): Pair<TextFieldState, FieldState<T>> {

    val fieldState by state.collectAsStateWithLifecycle()

    val textFieldState = rememberTextFieldState(
        initialText = toText(fieldState.value)
    )

    // UI → model
    LaunchedEffect(textFieldState.text) {
        val newValue = fromText(textFieldState.text.toString())
        if (newValue != fieldState.value) {
            update(newValue)
        }
    }

    // model → UI
    LaunchedEffect(fieldState.value) {
        val text = toText(fieldState.value)
        if (textFieldState.text.toString() != text) {
            textFieldState.setTextAndPlaceCursorAtEnd(text)
        }
    }

    return textFieldState to fieldState
}

@Composable
fun BucketConfiguration(
    bucketForm: BucketForm,
    verify: () -> Unit
) {
    val valid by bucketForm.valid().collectAsStateWithLifecycle(false)
    var passwordVisible by remember { mutableStateOf(false) }
    val warning by bucketForm.warning().collectAsStateWithLifecycle(false)


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
        Column(
            modifier =
            Modifier
                .padding(16.dp)
            //.imePadding() ?
        ) {
            Column {
                val focusRequesters = remember {
                    bucketForm.fields.associateWith { FocusRequester() }
                }

                LaunchedEffect(Unit) {
                    bucketForm.focusedField()?.let { field ->
                        focusRequesters[field]?.requestFocus()
                    }
                }

                val (nameTextState, nameState) =
                    bucketForm.name.rememberTextFieldState(
                        toText = { it },
                        fromText = { it }
                    )
                OutlinedTextField(
                    state = nameTextState,
                    modifier = Modifier
                        .onFocusChanged {
                            bucketForm.onFieldFocus(bucketForm.name, it)
                        }
                        .focusRequester(focusRequesters[bucketForm.name]!!)
                        .fillMaxWidth(),
                    label = { Text("Name") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = nameState.error != null,
                    supportingText = { nameState.error?.let { Text(it) } }
                )

                val (urlTextState, urlState) =
                    bucketForm.url.rememberTextFieldState(
                        toText = { it },
                        fromText = { it }
                    )
                OutlinedTextField(
                    state = urlTextState,
                    modifier = Modifier
                        .onFocusChanged { bucketForm.onFieldFocus(bucketForm.url, it) }
                        .focusRequester(focusRequesters[bucketForm.url]!!)
                        .fillMaxWidth(),
                    label = { Text("URL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = urlState.error != null,
                    supportingText = { urlState.error?.let { Text(it) } }
                )

                val (regionTextState, regionState) =
                    bucketForm.region.rememberTextFieldState(
                        toText = { it ?: "" },
                        fromText = { it.ifBlank { null } }
                    )
                OutlinedTextField(
                    state = regionTextState,
                    modifier = Modifier
                        .onFocusChanged { bucketForm.onFieldFocus(bucketForm.region, it) }
                        .focusRequester(focusRequesters[bucketForm.region]!!)
                        .fillMaxWidth(),
                    label = { Text("Region (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = regionState.error != null,
                    supportingText = { regionState.error?.let { Text(it) } }
                )

                val (keyTextState, keyState) =
                    bucketForm.key.rememberTextFieldState(
                        toText = { it },
                        fromText = { it }
                    )

                OutlinedTextField(
                    state = keyTextState,
                    modifier = Modifier
                        .onFocusChanged { bucketForm.onFieldFocus(bucketForm.key, it) }
                        .focusRequester(focusRequesters[bucketForm.key]!!)
                        .fillMaxWidth(),
                    label = { Text("Key") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = keyState.error != null,
                    supportingText = { keyState.error?.let { Text(it) } }
                )

                val (secretTextState, secretState) =
                    bucketForm.secret.rememberTextFieldState(
                        toText = { it },
                        fromText = { it }
                    )
                OutlinedTextField(
                    state = secretTextState,
                    modifier = Modifier
                        .onFocusChanged { bucketForm.onFieldFocus(bucketForm.secret, it) }
                        .focusRequester(focusRequesters[bucketForm.secret]!!)
                        .fillMaxWidth(),
                    label = { Text("Secret") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = secretState.error != null,
                    supportingText = { secretState.error?.let { Text(it) } }
                )

                val (bucketTextState, bucketState) =
                    bucketForm.bucket.rememberTextFieldState(
                        toText = { it },
                        fromText = { it }
                    )
                OutlinedTextField(
                    state = bucketTextState,
                    modifier = Modifier
                        .onFocusChanged { bucketForm.onFieldFocus(bucketForm.bucket, it) }
                        .focusRequester(focusRequesters[bucketForm.bucket]!!)
                        .fillMaxWidth(),
                    label = { Text("Bucket") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = bucketState.error != null,
                    supportingText = { bucketState.error?.let { Text(it) } }
                )
                val virtualHostedStyleState =
                    bucketForm.virtualHostedStyle.state.collectAsState()
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Virtual Hosted Style")
                            Text(
                                text = "Enable Virtual Hosted Style URLs instead of path-style.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, end = 4.dp)
                            )

                        }
                        Switch(
                            checked = virtualHostedStyleState.value.value,
                            onCheckedChange = {
                                bucketForm.virtualHostedStyle.touch()
                                bucketForm.virtualHostedStyle.update(it)
                            }
                        )
                    }
                    if (warning) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Virtual Hosted Style adds the bucket name to the start of the URL. If the URL already includes it, this may result in a duplicate.",
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            )
                        }
                    }
                }
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
