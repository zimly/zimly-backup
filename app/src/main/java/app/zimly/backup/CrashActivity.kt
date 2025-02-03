package app.zimly.backup

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import app.zimly.backup.ui.screens.crash.CrashScreen


private const val UNCAUGHT_EXCEPTION_MESSAGE = "Z_UNCAUGHT_EXCEPTION_MESSAGE"
private const val UNCAUGHT_EXCEPTION_STACK = "Z_UNCAUGHT_EXCEPTION_STACK"

class CrashActivity : ComponentActivity() {

    companion object {

        fun start(context: Context, throwable: Throwable) {
            val intent = Intent(context, CrashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            val extras = Bundle()
            extras.putString(UNCAUGHT_EXCEPTION_MESSAGE, throwable.message)
            extras.putString(UNCAUGHT_EXCEPTION_STACK, throwable.stackTraceToString())
            intent.putExtras(extras)
            context.startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent.extras?.getString(UNCAUGHT_EXCEPTION_MESSAGE)
        val stack = intent.extras?.getString(UNCAUGHT_EXCEPTION_STACK)

        setContent {
            CrashScreen(message, stack)
        }
    }

}
