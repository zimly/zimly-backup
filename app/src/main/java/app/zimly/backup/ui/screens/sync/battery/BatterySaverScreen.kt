package app.zimly.backup.ui.screens.sync.battery

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatterySaverViewModel(application: Application) : AndroidViewModel(application) {

    // Ensuring this is the application Context, not an Activity
    private val powerManager = application.getSystemService(android.os.PowerManager::class.java)
    private val batteryManager = application.getSystemService(android.os.BatteryManager::class.java)
    private val packageName = application.applicationContext.packageName

    private val _showWarning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showWarning: StateFlow<Boolean> = _showWarning.asStateFlow()
    private val _showDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private fun isCharging(): Boolean {
        return batteryManager.isCharging
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    fun updateBatteryState() {
        _showWarning.value = !(isIgnoringBatteryOptimizations() || isCharging())
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        context.startActivity(intent)
    }

    fun openDialog() {
        _showDialog.value = true
    }

    fun closeDialog() {
        _showDialog.value = false
    }

    companion object {

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                BatterySaverViewModel(application)
            }
        }
    }
}

@Composable
fun BatterySaverScreen(viewModel: BatterySaverViewModel = viewModel(factory = BatterySaverViewModel.Factory)) {

    val context = LocalContext.current.applicationContext as Application
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val showWarning by viewModel.showWarning.collectAsStateWithLifecycle()
    val showDialog by viewModel.showDialog.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        viewModel.updateBatteryState()
    }

    if (showWarning)
        BatterySaverWarning { viewModel.openDialog() }

    if (showDialog)
        BatterySaverInfoDialog({ viewModel.closeDialog() }, { viewModel.openSettings(context) })
}

@Composable
private fun BatterySaverWarning(openSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
        ),
        modifier = Modifier
            .fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .padding(1.dp)
                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.BatteryAlert,
                "Batter Alert",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Battery Saver Detected", modifier = Modifier.weight(1f))
            TextButton(
                onClick = { openSettings() },
                contentPadding = PaddingValues(
                    horizontal = 24.dp,
                    vertical = 1.dp
                ), // Reset padding
            ) {
                Text(text = "Learn More")
            }
        }
    }
}

@Composable
fun BatterySaverInfoDialog(
    closeDialog: () -> Unit,
    openSettings: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Outlined.BatteryAlert,
                contentDescription = "Battery Saver Alert",
                tint = MaterialTheme.colorScheme.error)
        },
        title = {
            Text(text = "Battery Saver Detected")
        },
        text = {
            Text(
                text = buildAnnotatedString {
                    append("Battery Saver is enabled, which may interrupt background synchronization.")
                    append("\n\nTo prevent this you can either:\n")
                    append("  • Plug in the charger\n")
                    append("  • Disable Battery Saver for Zimly")
                },
                textAlign = TextAlign.Left,
                modifier = Modifier.padding(start = 12.dp)

            )
        },
        onDismissRequest = {
            closeDialog()
        },
        confirmButton = {
            TextButton(onClick = { openSettings() }) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = { closeDialog() }
            ) {
                Text("Dismiss")
            }
        }
    )
}