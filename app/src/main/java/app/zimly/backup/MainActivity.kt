package app.zimly.backup

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.MutableCreationExtras
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

        // TODO: really?
        val viewModelStoreOwner: ViewModelStoreOwner = this
        val permissionViewModel: PermissionViewModel = ViewModelProvider.create(
            viewModelStoreOwner,
            factory = PermissionViewModel.Factory,
            extras = MutableCreationExtras().apply {
                set(APPLICATION_KEY, application)
                set(PermissionViewModel.CALLBACK_KEY) { finish() }
            },
        )[PermissionViewModel::class]

        setContent {
            ZimzyncTheme {
                AppNavigation(remoteDao, permissionViewModel)
            }
        }
    }

    @Composable
    private fun AppNavigation(remoteDao: RemoteDao, permissionViewModel: PermissionViewModel) {

        val granted = permissionViewModel.state.collectAsState()
        val startDest = if (granted.value.granted) REMOTES_LIST else GRANT_PERMISSION
        val navController = rememberNavController()

        NavHost(navController, startDestination = startDest) {

            // Grant permission for app
            // https://stackoverflow.com/questions/60608101/how-request-permissions-with-jetpack-compose
            composable(GRANT_PERMISSION) {
                PermissionScreen(permissionViewModel)
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


}
