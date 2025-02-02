package app.zimly.backup

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import app.zimly.backup.ui.screens.crash.CrashScreen


private const val UNCAUGHT_EXCEPTION = "Z_UNCAUGHT_EXCEPTION"

class CrashActivity : ComponentActivity() {


    companion object {
        private val TAG: String? = CrashActivity::class.simpleName

        fun start(context: Context, throwable: Throwable) {
            val intent = Intent(context, CrashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            val extras = Bundle()
            extras.putSerializable(UNCAUGHT_EXCEPTION, throwable)
            intent.putExtras(extras)
            context.startActivity(intent)
        }

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val throwable = intent.extras?.getSerializable(UNCAUGHT_EXCEPTION, Throwable::class.java)

        setContent {
            CrashScreen(throwable)
        }
    }

    class ExceptionHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

        override fun uncaughtException(t: Thread, e: Throwable) {
            Log.e(TAG, "Uncaught Exception: ${e.message}", e)
            start(context, e)
        }

    }
}
