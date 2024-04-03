package io.zeitmaschine.zimzync

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import io.zeitmaschine.zimzync.ui.screens.editor.EditorScreen
import io.zeitmaschine.zimzync.ui.screens.list.RemoteScreen
import io.zeitmaschine.zimzync.ui.screens.sync.SyncRemote
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "FlowOperatorInvokedInComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, ZimDatabase::class.java, "zim-db")
            .build()
        val remoteDao = db.remoteDao()

        setContent {
            ZimzyncTheme {
                val startDest by remember { mutableStateOf(if (isPermissionGranted()) "remotes-list" else "grant-permission") }
                val navController = rememberNavController()
                NavHost(navController, startDestination = startDest) {
                    // Grant permission for app
                    // https://stackoverflow.com/questions/60608101/how-request-permissions-with-jetpack-compose
                    composable("grant-permission") {
                        val permissionLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestMultiplePermissions()
                        ) { isGranted ->

                            // Lookup and compare the granted permission
                            val granted = getPermissions()
                                .map { permission -> isGranted[permission] }
                                .reduce { granted, permission -> granted == true && permission == true }

                            if (granted == true) {
                                Log.i(localClassName, "Permissions granted")
                                navController.navigate("remotes-list")
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

                    composable("remotes-list") {
                        Scaffold(
                            floatingActionButtonPosition = FabPosition.End,
                            floatingActionButton = {
                                FloatingActionButton(onClick = {
                                    navController.navigate("remote-editor/create")
                                }) {
                                    Icon(Icons.Filled.Add, "Add Remote")
                                }
                            },
                            content = {
                                RemoteScreen(
                                    remoteDao,
                                    openSync = { remoteId -> navController.navigate("remote-sync?remoteId=$remoteId") })
                            })
                    }

                    composable(
                        "remote-editor/edit/{remoteId}",
                        arguments = listOf(navArgument("remoteId") { nullable = false })
                    ) { backStackEntry ->
                        val remoteId = backStackEntry.arguments?.getString("remoteId")?.toInt()
                        Scaffold(
                            content = {
                                EditorScreen(
                                    application = application,
                                    remoteDao = remoteDao,
                                    remoteId = remoteId,
                                    back = { navController.popBackStack() }
                                )
                            },
                        )
                    }

                    composable(
                        "remote-editor/create",
                        arguments = listOf(navArgument("remoteId") { nullable = true })
                    ) {
                        Scaffold(
                            content = {
                                EditorScreen(
                                    application = application,
                                    remoteDao = remoteDao,
                                    remoteId = null,
                                    back = { navController.popBackStack() }
                                )
                            },
                        )
                    }

                    composable(
                        "remote-sync?remoteId={remoteId}",
                        arguments = listOf(navArgument("remoteId") { nullable = false })
                    ) { backStackEntry ->
                        Scaffold {
                            val remoteId = backStackEntry.arguments?.getString("remoteId")?.toInt()

                            remoteId?.let {
                                SyncRemote(
                                    remoteDao,
                                    remoteId,
                                    application = application,
                                    edit = { remoteId -> navController.navigate("remote-editor/edit/${remoteId}") },
                                    back = { navController.popBackStack() })
                            }
                        }
                    }
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
