package app.zimly.backup

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.zimly.backup.permission.PermissionService
import app.zimly.backup.ui.screens.editor.EditorScreen
import app.zimly.backup.ui.screens.start.StartScreen
import app.zimly.backup.ui.screens.permission.PermissionRequestScreen
import app.zimly.backup.ui.screens.sync.SyncScreen
import app.zimly.backup.ui.theme.ZimzyncTheme


private const val REMOTES_LIST = "remotes-list"

class MainActivity : ComponentActivity() {

    companion object {
        private val TAG: String? = MainActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _: Thread, throwable: Throwable ->
            Log.e(TAG, "Unhandled Exception!", throwable)

            CrashActivity.start(applicationContext, throwable)
            finish()
        }

        val permissionService = PermissionService(applicationContext, application.packageName)
        setContent {
            ZimzyncTheme {
                AppNavigation(permissionService.permissionsGranted())
            }
        }
    }

    @Composable
    private fun AppNavigation(grantedPermissions: Boolean) {

        var permissionRequest by remember { mutableStateOf(!grantedPermissions) }
        val navController = rememberNavController()

        NavHost(navController, startDestination = REMOTES_LIST) {


            composable(REMOTES_LIST) {
                if (permissionRequest) {
                    PermissionRequestScreen({ permissionRequest = false })
                }
                StartScreen(
                    syncRemote = { remoteId -> navController.navigate("remote-sync?remoteId=$remoteId") },
                    addRemote = { navController.navigate("remote-editor/create") })
            }

            composable(
                "remote-editor/edit/{remoteId}",
                arguments = listOf(navArgument("remoteId") { nullable = false })
            ) { backStackEntry ->
                val remoteId = backStackEntry.arguments?.getString("remoteId")?.toInt()
                EditorScreen(
                    remoteId = remoteId,
                    back = { navController.popBackStack() }
                )
            }

            composable(
                "remote-editor/create",
                arguments = listOf(navArgument("remoteId") { nullable = true })
            ) {
                EditorScreen(
                    remoteId = null,
                    back = { navController.popBackStack() }
                )
            }

            composable(
                "remote-sync?remoteId={remoteId}",
                arguments = listOf(navArgument("remoteId") { nullable = false })
            ) { backStackEntry ->
                val remoteId = backStackEntry.arguments?.getString("remoteId")?.toInt()

                remoteId?.let {
                    SyncScreen(
                        remoteId,
                        edit = { remoteId -> navController.navigate("remote-editor/edit/${remoteId}") },
                        back = { navController.popBackStack() })
                }
            }
        }
    }


}
