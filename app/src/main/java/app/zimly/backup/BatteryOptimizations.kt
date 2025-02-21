package app.zimly.backup

import android.app.Application
import android.content.Intent
import android.provider.Settings

class BatteryOptimizations(application: Application) {

    // Ensuring this is the application Context, not an Activity
    private val context = application.applicationContext
    private val powerManager = context.getSystemService(android.os.PowerManager::class.java)

    fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openSettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }
}