package io.zeitmaschine.zimzync

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
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
                val navController = rememberNavController()
                NavHost(navController, startDestination = "remotes-list") {
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
                                RemoteScreen(LocalContext.current.remoteDataStore, editEntry = { remoteId -> navController.navigate("remote-editor?remoteId=$remoteId") })
                            })
                    }

                    composable("remote-editor?remoteId={remoteId}", arguments = listOf(navArgument("remoteId") { nullable = true })
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
                }
            }
        }
    }
}
