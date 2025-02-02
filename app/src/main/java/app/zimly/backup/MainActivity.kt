package app.zimly.backup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import app.zimly.backup.ui.screens.sync.SyncScreen
import app.zimly.backup.ui.theme.ZimzyncTheme

private const val REMOTES_LIST = "remotes-list"
private const val GRANT_PERMISSION = "grant-permission"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, ZimDatabase::class.java, "zim-db")
            .build()
        val remoteDao = db.remoteDao()

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
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { isGranted ->

                    // Lookup and compare the granted permission
                    val granted = getPermissions()
                        .map { permission -> isGranted[permission] }
                        .reduce { granted, permission -> granted == true && permission == true }

                    if (granted == true) {
                        Log.i(localClassName, "Permissions granted")
                        startDest = REMOTES_LIST
                    } else {
                        // TODO Implement some sort of informative screen, that the user
                        // needs to grant permissions for the app to work.
                        Log.i(localClassName, "PERMISSION DENIED")
                    }
                }
                SideEffect {
                    permissionLauncher.launch(
                        getPermissions()
                    )
                }
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
        val granted = getPermissions()
            .map { permission ->
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
            .reduce { granted, permission -> granted && permission }

        return granted
    }

    /*
     * Returns the needed permissions based on Android SDK version.
     * Changes were introduced to the media permissions in API 33+
     */
    private fun getPermissions(): Array<String> {
        val permissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Compatibility with older versions
            arrayOf(
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else {
            // New permissions
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
        }
        return permissions
    }
}
