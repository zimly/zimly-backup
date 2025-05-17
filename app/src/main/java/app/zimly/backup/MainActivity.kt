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
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.zimly.backup.data.db.remote.SyncDirection
import app.zimly.backup.permission.PermissionService
import app.zimly.backup.ui.screens.editor.steps.BucketConfigurationStep
import app.zimly.backup.ui.screens.editor.steps.DownloadTargetStep
import app.zimly.backup.ui.screens.editor.steps.SyncDirectionStep
import app.zimly.backup.ui.screens.editor.steps.UploadSourceStep
import app.zimly.backup.ui.screens.editor.wizardViewModel
import app.zimly.backup.ui.screens.permission.PermissionRequestScreen
import app.zimly.backup.ui.screens.start.StartScreen
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
                    syncRemote = { remoteId, direction ->
                        when (direction) {
                            SyncDirection.UPLOAD -> navController.navigate("upload-sync?remoteId=$remoteId")
                            SyncDirection.DOWNLOAD -> navController.navigate("download-sync?remoteId=$remoteId")
                        }
                    },
                    addRemote = { navController.navigate("wizard") })
            }

            navigation(
                route = "wizard",
                startDestination = "wizard/direction",
            ) {

                composable("wizard/direction") {

                    val vm = navController.wizardViewModel(null)
                    SyncDirectionStep(
                        store = vm.directionStore,
                        nextStep = { direction ->
                            when (direction) {
                                SyncDirection.UPLOAD -> navController.navigate("wizard/upload")
                                SyncDirection.DOWNLOAD -> navController.navigate("wizard/download")
                            }
                        },
                        previousStep = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "wizard/upload?id={id}",
                    arguments = listOf(navArgument("id") { nullable = true })
                ) { backStackEntry ->

                    val remoteId = backStackEntry.arguments?.getString("id")?.toInt()

                    val vm = navController.wizardViewModel(remoteId)

                    UploadSourceStep(
                        store = vm.contentStore,
                        nextStep = { navController.navigate("wizard/bucket?id=${remoteId}") },
                        previousStep = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "wizard/download?id={id}",
                    arguments = listOf(navArgument("id") { nullable = true })
                ) { backStackEntry ->

                    val remoteId = backStackEntry.arguments?.getString("id")?.toInt()

                    val vm = navController.wizardViewModel(remoteId)
                    DownloadTargetStep(
                        store = vm.contentStore,
                        nextStep = { navController.navigate("wizard/bucket?id=${remoteId}") },
                        previousStep = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "wizard/bucket?id={id}",
                    arguments = listOf(navArgument("id") { nullable = true })
                ) { backStackEntry ->
                    val remoteId = backStackEntry.arguments?.getString("remoteId")?.toInt()

                    val vm = navController.wizardViewModel(remoteId)
                    BucketConfigurationStep(
                        store = vm.bucketStore,
                        vm,
                        nextStep = { navController.popBackStack(REMOTES_LIST, inclusive = false) },
                        previousStep = { navController.popBackStack() },
                    )
                }
            }

            composable(
                "upload-sync?remoteId={remoteId}",
                arguments = listOf(navArgument("remoteId") { nullable = false })
            ) { backStackEntry ->
                val remoteId = backStackEntry.arguments?.getString("remoteId")?.toInt()

                remoteId?.let {
                    SyncScreen(
                        remoteId,
                        edit = { direction, remoteId ->
                            when (direction) {
                                SyncDirection.UPLOAD -> navController.navigate("wizard/upload?id=${remoteId}")
                                SyncDirection.DOWNLOAD -> navController.navigate("wizard/download?id=${remoteId}")
                                null -> {} // Might not have loaded in time
                            }
                        },
                        back = { navController.popBackStack() }
                    )
                }
            }
            composable(
                "download-sync?remoteId={remoteId}",
                arguments = listOf(navArgument("remoteId") { nullable = false })
            ) { backStackEntry ->
                val remoteId = backStackEntry.arguments?.getString("remoteId")?.toInt()

                remoteId?.let {
                    SyncScreen(
                        remoteId,
                        edit = { direction, remoteId ->
                            when (direction) {
                                SyncDirection.UPLOAD -> navController.navigate("wizard/upload?id=${remoteId}")
                                SyncDirection.DOWNLOAD -> navController.navigate("wizard/download?id=${remoteId}")
                                null -> {} // Might not have loaded in time
                            }
                        },
                        back = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
