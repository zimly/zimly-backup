package app.zimly.backup.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Composable
fun NotificationBar(
    notificationProvider: NotificationProvider,
    snackbarState: SnackbarHostState
) {

    val notification by notificationProvider.get().collectAsStateWithLifecycle(null)

    notification?.let {
        LaunchedEffect(snackbarState) {
            val result = snackbarState.showSnackbar(
                message = it.message,
                withDismissAction = true,
                duration = when (it.type) {
                    Notification.Type.INFO -> SnackbarDuration.Short
                    Notification.Type.ERROR -> SnackbarDuration.Indefinite
                }
            )
            when (result) {
                SnackbarResult.Dismissed -> notificationProvider.reset()
                SnackbarResult.ActionPerformed -> notificationProvider.reset()
            }
        }
    }
}

data class Notification(val message: String, val type: Type) {
    enum class Type {
        INFO, ERROR
    }
}

interface NotificationProvider {
    fun reset()
    fun get(): Flow<Notification?>

    fun combine(vararg providers: NotificationProvider): NotificationProvider {
        return CombinedProvider(arrayOf(this, *providers))
    }
}

/**
 * A delegating [NotificationProvider] that combines all [providers] into one.
 */
class CombinedProvider(private val providers: Array<NotificationProvider>) : NotificationProvider {

    override fun reset() {
        providers.forEach { it.reset() }
    }

    override fun get(): Flow<Notification?> {
        return combine(providers.map { it.get() }) { notifications: Array<Notification?> ->
            notifications.firstOrNull { it != null }
        }
    }
}
