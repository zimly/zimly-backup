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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatterySaverViewModel(application: Application): AndroidViewModel(application) {

    // Ensuring this is the application Context, not an Activity
    private val powerManager = application.getSystemService(android.os.PowerManager::class.java)
    private val packageName = application.applicationContext.packageName

    private val _showWarning: MutableStateFlow<Boolean> = MutableStateFlow(!isIgnoringBatteryOptimizations())
    val showWarning: StateFlow<Boolean> = _showWarning.asStateFlow()

    private fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
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
    val showWarning by viewModel.showWarning.collectAsStateWithLifecycle()

    if (showWarning)
        Battery { viewModel.openSettings(context) }
}

@Composable
private fun Battery(openSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
        ),
        modifier = Modifier
            .fillMaxWidth()
    ) {

        Row (modifier = Modifier
            .padding(1.dp)
            .fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.BatteryAlert,
                "Progress",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Battery Saver detected", modifier = Modifier.weight(1f))
            TextButton(
                onClick = { openSettings() },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 1.dp), // Reset padding
            ) {
                Text(text = "Disable")
            }

        }
    }
}
