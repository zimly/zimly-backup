package app.zimly.backup

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.zimly.backup.permission.PermissionService
import app.zimly.backup.ui.screens.editor.EditorScreen
import app.zimly.backup.ui.screens.list.ListScreen
import app.zimly.backup.ui.screens.sync.SyncScreen
import app.zimly.backup.ui.theme.ZimzyncTheme


private const val REMOTES_LIST = "remotes-list"
private const val GRANT_PERMISSION = "grant-permission"

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

        val permissionService = PermissionService(applicationContext)
        setContent {
            ZimzyncTheme {
                AppNavigation(permissionService.isPermissionGranted(), permissionService)
            }
        }
    }

    @Composable
    private fun AppNavigation(grantedPermissions: Boolean, permissionService: PermissionService) {

        val showDialog = remember { mutableStateOf(!grantedPermissions) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->
            Log.d("PermissionBox", "Permissions grants: $grants")
            showDialog.value = false
        }

        val startDest = if (showDialog.value) GRANT_PERMISSION else REMOTES_LIST
        val navController = rememberNavController()


        NavHost(navController, startDestination = startDest) {

            // Grant permission for app
            // https://stackoverflow.com/questions/60608101/how-request-permissions-with-jetpack-compose
            composable(GRANT_PERMISSION) {
                LaunchedEffect(showDialog) {
                    permissionLauncher.launch(permissionService.getPermissions())
                }
            }


            composable(REMOTES_LIST) {
                ListScreen(
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
