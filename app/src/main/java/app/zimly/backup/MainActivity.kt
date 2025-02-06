package app.zimly.backup

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import app.zimly.backup.data.remote.RemoteDao
import app.zimly.backup.data.remote.ZimDatabase
import app.zimly.backup.ui.screens.editor.EditorScreen
import app.zimly.backup.ui.screens.list.ListScreen
import app.zimly.backup.ui.screens.permission.PermissionScreen
import app.zimly.backup.ui.screens.permission.PermissionViewModel
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
        val db = Room.databaseBuilder(applicationContext, ZimDatabase::class.java, "zim-db")
            .build()
        val remoteDao = db.remoteDao()

        Thread.setDefaultUncaughtExceptionHandler { _: Thread, throwable: Throwable ->
            Log.wtf(TAG, "Unhandled Exception!", throwable)

            CrashActivity.start(applicationContext, throwable)
            finish()
        }


        setContent {
            ZimzyncTheme {
                AppNavigation(remoteDao)
            }
        }
    }

    @Composable
    private fun AppNavigation(remoteDao: RemoteDao) {

        var startDest by remember { mutableStateOf(if (isPermissionGranted()) REMOTES_LIST else GRANT_PERMISSION) }
        val navController = rememberNavController()

        NavHost(navController, startDestination = startDest) {

            // Grant permission for app
            // https://stackoverflow.com/questions/60608101/how-request-permissions-with-jetpack-compose
            composable(GRANT_PERMISSION) {
                PermissionScreen({startDest = REMOTES_LIST})
            }

            composable(REMOTES_LIST) {
                ListScreen(
                    remoteDao,
                    syncRemote = { remoteId -> navController.navigate("remote-sync?remoteId=$remoteId") },
                    addRemote = { navController.navigate("remote-editor/create") })
            }

            composable(
                "remote-editor/edit/{remoteId}",
                arguments = listOf(navArgument("remoteId") { nullable = false })
            ) { backStackEntry ->
                val remoteId = backStackEntry.arguments?.getString("remoteId")?.toInt()
                EditorScreen(
                    application = application,
                    remoteDao = remoteDao,
                    remoteId = remoteId,
                    back = { navController.popBackStack() }
                )
            }

            composable(
                "remote-editor/create",
                arguments = listOf(navArgument("remoteId") { nullable = true })
            ) {
                EditorScreen(
                    application = application,
                    remoteDao = remoteDao,
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
                        remoteDao,
                        remoteId,
                        application = application,
                        edit = { remoteId -> navController.navigate("remote-editor/edit/${remoteId}") },
                        back = { navController.popBackStack() })
                }
            }
        }
    }

    // Check initially if the permission is granted
    private fun isPermissionGranted(): Boolean {
        val granted = PermissionViewModel.getPermissions()
            .map { permission ->
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
            .reduce { granted, permission -> granted && permission }

        return granted
    }


}
