package app.zimly.backup

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import app.zimly.backup.data.db.sync.SyncDirection
import app.zimly.backup.permission.MediaPermissionService
import app.zimly.backup.ui.screens.editor.editorViewModel
import app.zimly.backup.ui.screens.editor.steps.BucketConfigurationStep
import app.zimly.backup.ui.screens.editor.steps.DownloadTargetStep
import app.zimly.backup.ui.screens.editor.steps.SyncDirectionStep
import app.zimly.backup.ui.screens.editor.steps.UploadSourceStep
import app.zimly.backup.ui.screens.permission.PermissionRequestScreen
import app.zimly.backup.ui.screens.start.StartScreen
import app.zimly.backup.ui.screens.sync.SyncScreen
import app.zimly.backup.ui.theme.ZimzyncTheme


private const val SYNC_PROFILES = "sync-profiles"

class MainActivity : ComponentActivity() {

    companion object {
        private val TAG: String? = MainActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        Thread.setDefaultUncaughtExceptionHandler { _: Thread, throwable: Throwable ->
            Log.e(TAG, "Unhandled Exception!", throwable)

            CrashActivity.start(applicationContext, throwable)
            finish()
        }

        val mediaPermissionService = MediaPermissionService(applicationContext, application.packageName)
        setContent {
            ZimzyncTheme {
                AppNavigation(mediaPermissionService.permissionsGranted())
            }
        }
    }

    @Composable
    private fun AppNavigation(grantedPermissions: Boolean) {

        var permissionRequest by remember { mutableStateOf(!grantedPermissions) }
        val navController = rememberNavController()

        NavHost(navController, startDestination = SYNC_PROFILES) {

            composable(SYNC_PROFILES) {
                if (permissionRequest) {
                    PermissionRequestScreen({ permissionRequest = false })
                }
                StartScreen(
                    openSyncProfile = { syncProfileId, direction ->
                        when (direction) {
                            SyncDirection.UPLOAD -> navController.navigate("upload-sync?syncProfileId=$syncProfileId")
                            SyncDirection.DOWNLOAD -> navController.navigate("download-sync?syncProfileId=$syncProfileId")
                        }
                    },
                    addSyncProfile = { navController.navigate("wizard") })
            }

            navigation(
                route = "wizard",
                startDestination = "wizard/direction",
            ) {

                composable("wizard/direction") {

                    val vm = navController.editorViewModel(null)
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

                    val syncProfileId = backStackEntry.arguments?.getString("id")?.toInt()

                    val vm = navController.editorViewModel(syncProfileId)

                    UploadSourceStep(
                        store = vm.contentStore,
                        nextStep = { navController.navigate("wizard/bucket?id=${syncProfileId}") },
                        previousStep = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "wizard/download?id={id}",
                    arguments = listOf(navArgument("id") { nullable = true })
                ) { backStackEntry ->

                    val syncProfileId = backStackEntry.arguments?.getString("id")?.toInt()

                    val vm = navController.editorViewModel(syncProfileId)
                    DownloadTargetStep(
                        store = vm.contentStore,
                        nextStep = { navController.navigate("wizard/bucket?id=${syncProfileId}") },
                        previousStep = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "wizard/bucket?id={id}",
                    arguments = listOf(navArgument("id") { nullable = true })
                ) { backStackEntry ->
                    val syncProfileId = backStackEntry.arguments?.getString("syncProfileId")?.toInt()

                    val vm = navController.editorViewModel(syncProfileId)
                    BucketConfigurationStep(
                        store = vm.bucketStore,
                        vm,
                        nextStep = { navController.popBackStack(SYNC_PROFILES, inclusive = false) },
                        previousStep = { navController.popBackStack() },
                    )
                }
            }

            composable(
                "upload-sync?syncProfileId={syncProfileId}",
                arguments = listOf(navArgument("syncProfileId") { nullable = false })
            ) { backStackEntry ->
                val syncProfileId = backStackEntry.arguments?.getString("syncProfileId")?.toInt()

                syncProfileId?.let {
                    SyncScreen(
                        syncProfileId,
                        edit = { direction, syncProfileId ->
                            when (direction) {
                                SyncDirection.UPLOAD -> navController.navigate("wizard/upload?id=${syncProfileId}")
                                SyncDirection.DOWNLOAD -> navController.navigate("wizard/download?id=${syncProfileId}")
                                null -> {} // Might not have loaded in time
                            }
                        },
                        back = { navController.popBackStack() }
                    )
                }
            }
            composable(
                "download-sync?syncProfileId={syncProfileId}",
                arguments = listOf(navArgument("syncProfileId") { nullable = false })
            ) { backStackEntry ->
                val syncProfileId = backStackEntry.arguments?.getString("syncProfileId")?.toInt()

                syncProfileId?.let {
                    SyncScreen(
                        syncProfileId,
                        edit = { direction, syncProfileId ->
                            when (direction) {
                                SyncDirection.UPLOAD -> navController.navigate("wizard/upload?id=${syncProfileId}")
                                SyncDirection.DOWNLOAD -> navController.navigate("wizard/download?id=${syncProfileId}")
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
