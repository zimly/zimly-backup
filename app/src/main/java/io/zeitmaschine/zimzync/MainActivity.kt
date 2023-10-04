package io.zeitmaschine.zimzync

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
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
                var startDest by remember { mutableStateOf(if (isPermissionGranted()) "remotes-list" else "grant-permission") }
                val navController = rememberNavController()
                NavHost(navController, startDestination = startDest) {
                    // Grant permission for app
                    // https://stackoverflow.com/questions/60608101/how-request-permissions-with-jetpack-compose
                    // https://semicolonspace.com/jetpack-compose-request-permissions/#rememberLauncherForActivityResult
                    composable("grant-permission") {
                        val permissionLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestMultiplePermissions()
                        ) { isGranted ->
                            if (isGranted[Manifest.permission.READ_MEDIA_IMAGES] == true && isGranted[Manifest.permission.READ_MEDIA_VIDEO] == true && isGranted[Manifest.permission.ACCESS_MEDIA_LOCATION] == true) {
                                Log.i(localClassName, "Permissions granted")
                                navController.navigate("remotes-list")
                            } else {
                                Log.i(localClassName, "PERMISSION DENIED")
                            }
                        }
                        SideEffect {
                            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.ACCESS_MEDIA_LOCATION))
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
                                EditRemote(
                                    application = application,
                                    remoteDao = remoteDao,
                                    remoteId = remoteId,
                                    saveEntry = { navController.navigate("remotes-list") }
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
                                EditRemote(
                                    application = application,
                                    remoteDao = remoteDao,
                                    remoteId = null,
                                    saveEntry = { navController.navigate("remotes-list")}
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
                                SyncRemote(remoteDao, remoteId, application = application, edit = { remoteId -> navController.navigate("remote-editor/edit/${remoteId}") })
                            }
                        }
                    }

                }
            }
        }
    }

    // check initially if the permission is granted
    private fun isPermissionGranted(): Boolean {
        val location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
        val photos = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val videos = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        return photos && videos && location
    }
}
