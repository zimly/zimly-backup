package app.zimly.backup.ui.screens.sync.battery

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import app.zimly.backup.data.db.ZimlyDatabase
import app.zimly.backup.data.db.notification.Notification
import app.zimly.backup.data.db.notification.NotificationDao
import app.zimly.backup.data.db.notification.NotificationType
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BatterySaverViewModel(
    private val powerStatus: PowerStatusProvider,
    private val notificationDao: NotificationDao
) : ViewModel() {

    private val _showWarning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showWarning: StateFlow<Boolean> = _showWarning.asStateFlow()
    private val _showDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    init {
        viewModelScope.launch {
            val batteryWarning = !(powerStatus.isBatterSaverDisabled() || powerStatus.isCharging())
            _showWarning.value = batteryWarning && !isIgnoreWarning()
        }
    }

    private suspend fun isIgnoreWarning(): Boolean {
        val read = notificationDao.loadByType(NotificationType.BATTERY_SAVER_WARNING)?.ignore ?: false
        return read
    }

    private suspend fun setIgnoreWarning() {
        val notification = notificationDao.loadByType(NotificationType.BATTERY_SAVER_WARNING)
        if (notification == null) {
            notificationDao.insert(
                Notification(
                    uid = null,
                    type = NotificationType.BATTERY_SAVER_WARNING,
                    ignore = true
                )
            )
        } else {
            val updated = Notification(notification.uid, notification.type, true)
            notificationDao.update(updated)
        }
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        context.startActivity(intent)
    }

    fun openDialog() {
        _showDialog.value = true
    }

    suspend fun closeDialog(ignore: Boolean) {
        _showDialog.value = false
        _showWarning.value = false
        if (ignore) {
            setIgnoreWarning()
        }
    }

    companion object {

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                val db = Room.databaseBuilder(
                    application.applicationContext,
                    ZimlyDatabase::class.java,
                    "zim-db"
                )
                    .build()
                val notificationDao = db.notificationDao()

                val powerManager = application.getSystemService(PowerManager::class.java)
                val batteryManager =
                    application.getSystemService(BatteryManager::class.java)
                val powerStatus = PowerStatusProviderImpl(
                    powerManager,
                    batteryManager,
                    application.applicationContext.packageName
                )

                BatterySaverViewModel(powerStatus, notificationDao)
            }
        }
    }
}

@Composable
fun BatterySaverContainer(viewModel: BatterySaverViewModel = viewModel(factory = BatterySaverViewModel.Factory)) {

    val context = LocalContext.current.applicationContext
    val showWarning by viewModel.showWarning.collectAsStateWithLifecycle()
    val showDialog by viewModel.showDialog.collectAsStateWithLifecycle()

    if (showWarning)
        BatterySaverWarning { viewModel.openDialog() }

    if (showDialog)
        BatterySaverInfoDialog(
            { viewModel.viewModelScope.launch { viewModel.closeDialog(it) } },
            { viewModel.openSettings(context) })
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
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.BatteryAlert,
                "Battery Alert",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text("Battery Saver Detected", modifier = Modifier.weight(1f))
            TextButton(
                onClick = { openSettings() },
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                ), // Reset padding
            ) {
                Text(text = "Learn More")
            }
        }
    }
}

@Composable
fun BatterySaverInfoDialog(
    closeDialog: (ignore: Boolean) -> Unit,
    openSettings: () -> Unit,
) {
    var checked by remember { mutableStateOf(false) }
    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Outlined.BatteryAlert,
                contentDescription = "Battery Saver Alert",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = "Battery Saver Detected")
        },
        text = {
            Column {

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

                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it }
                    )
                    Text(
                        "Don't show again"
                    )
                }
            }
        },
        onDismissRequest = {
            closeDialog(checked)
        },
        confirmButton = {
            TextButton(onClick = { openSettings() }) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = { closeDialog(checked) }
            ) {
                Text("Dismiss")
            }
        }
    )
}