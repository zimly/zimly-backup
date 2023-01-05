package io.zeitmaschine.zimzync

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.zeitmaschine.zimzync.ui.theme.ZimzyncTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZimzyncTheme {
                val startDest = if (isPermissionGranted()) "remotes-list" else "grant-permission"
                val navController = rememberNavController()
                NavHost(navController, startDestination = startDest) {

                    composable("grant-permission") {
                        /*

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Permission Accepted: Do something
            Log.d("ExampleScreen","PERMISSION GRANTED")

        } else {
            // Permission Denied: Do something
            Log.d("ExampleScreen","PERMISSION DENIED")
        }
    }
    val context = LocalContext.current
    when (PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) -> {
            // Some works that require permission
            Log.d("ExampleScreen","Code requires permission")
        }
        else -> {
            // Asking for permission
            SideEffect {
                launcher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }
                         */

                        // https://semicolonspace.com/jetpack-compose-request-permissions/#rememberLauncherForActivityResult
                        // https://stackoverflow.com/questions/60608101/how-request-permissions-with-jetpack-compose
                        // https://stackoverflow.com/questions/68331511/rememberlauncherforactivityresult-throws-launcher-has-not-been-initialized-in-je
                        var permissionGranted by remember {
                            mutableStateOf(isPermissionGranted())
                        }

                        val permissionLauncher =
                            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { permissionGranted_ ->
                                // this is called when the user selects allow or deny
                                Toast.makeText(
                                    this@MainActivity,
                                    "permissionGranted_ $permissionGranted_",
                                    Toast.LENGTH_SHORT
                                ).show()
                                permissionGranted = permissionGranted_
                            }

                        Surface(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!permissionGranted)
                                    SideEffect {
                                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                    }
                                else {
                                    // update your UI
                                    navController.navigate("remotes-list")
                                }
                            }
                        }
                    }

                    composable("remotes-list") {
                        Scaffold(
                            floatingActionButtonPosition = FabPosition.End,
                            floatingActionButton = {
                                FloatingActionButton(onClick = {
                                    navController.navigate("remote-editor")
                                }) {
                                    Icon(Icons.Filled.Add, "Add Remote")
                                }
                            },
                            content = {
                                RemoteScreen(
                                    LocalContext.current.remoteDataStore,
                                    openSync = { remoteId -> navController.navigate("remote-sync?remoteId=$remoteId") })
                            })
                    }

                    composable(
                        "remote-editor?remoteId={remoteId}",
                        arguments = listOf(navArgument("remoteId") { nullable = true })
                    ) { backStackEntry ->
                        Scaffold(
                            content = {
                                EditRemote(
                                    dataStore = LocalContext.current.remoteDataStore,
                                    remoteId = backStackEntry.arguments?.getString("remoteId"),
                                    saveEntry = { navController.navigate("remotes-list") }
                                )
                            },
                        )
                    }

                    composable(
                        "remote-sync?remoteId={remoteId}",
                        arguments = listOf(navArgument("remoteId") { nullable = false })
                    ) { backStackEntry ->
                        Scaffold(
                            content = {
                                SyncRemote(
                                    dataStore = LocalContext.current.remoteDataStore,
                                    remoteId = backStackEntry.arguments?.getString("remoteId")
                                )
                            }
                        )
                    }

                }
            }
        }
    }

    // check initially if the permission is granted
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
