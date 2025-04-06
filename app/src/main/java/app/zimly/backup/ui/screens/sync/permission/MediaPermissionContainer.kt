package app.zimly.backup.ui.screens.sync.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.permission.PermissionService
import app.zimly.backup.ui.components.PermissionRationaleDialog
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaPermissionViewModel(
    private val permissionService: PermissionService
) : ViewModel() {

    private val _showWarning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showWarning: StateFlow<Boolean> = _showWarning.asStateFlow()
    private val _showDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    init {
        updateState()
    }

    fun openSettings(context: Context) {
        permissionService.openSettings(context)
    }

    fun openDialog() {
        _showDialog.value = true
    }

    fun closeDialog() {
        _showDialog.value = false
    }

    fun getPermission(): Array<String> {
        return permissionService.getPermissions()
    }

    fun isAnyPermissionPermanentlyDenied(context: Context): Boolean {
        if (context !is Activity) {
            Log.e(TAG, "Expected an Activity as Context object but got: ${context.javaClass.name}")
            return false
        }
        return permissionService.isAnyPermissionPermanentlyDenied(context)
    }

    fun onGranted(grants: Map<String, Boolean>) {
        val allGranted = permissionService.checkUserGrants(grants)
        _showWarning.value = allGranted != true

    }

    fun updateState() {
        val granted = permissionService.isPermissionGranted()
        _showWarning.value = !granted
    }

    companion object {

        private val TAG: String? = MediaPermissionViewModel::class.simpleName

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])

                val permissionService = PermissionService(
                    application.applicationContext,
                    application.packageName
                )
                MediaPermissionViewModel(permissionService)
            }
        }
    }
}

@Composable
fun MediaPermissionContainer(viewModel: MediaPermissionViewModel = viewModel(factory = MediaPermissionViewModel.Factory)) {

    val context = LocalContext.current
    val showWarning by viewModel.showWarning.collectAsStateWithLifecycle()
    val showDialog by viewModel.showDialog.collectAsStateWithLifecycle()
    // TODO remember
    val permissionsDenied = viewModel.isAnyPermissionPermanentlyDenied(context)

    // Observe lifecycle changes to trigger the permission check when the activity resumes
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateState() // Check permission on resume
            }
        }

        // Adding observer
        lifecycleOwner.lifecycle.addObserver(observer)

        // Cleanup when the composable leaves the composition
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showWarning)
        PermissionWarning { viewModel.openDialog() }

    if (showDialog)
        PermissionRationaleDialog (
            permissionsDenied,
            viewModel.getPermission(),
            { viewModel.closeDialog() },
            { viewModel.onGranted(it) },
            { viewModel.openSettings(context) })
}

@Composable
private fun PermissionWarning(openDialog: () -> Unit) {
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
                Icons.Outlined.Lock,
                "Media Permission Alert",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text("Missing Media Permissions", modifier = Modifier.weight(1f))
            TextButton(
                onClick = { openDialog() },
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                ), // Reset padding
            ) {
                Text(text = "Learn More")
            }
        }
    }
}
