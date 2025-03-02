package app.zimly.backup.ui.screens.sync.battery

import android.os.BatteryManager
import android.os.PowerManager

interface PowerStatusProvider {

    fun isCharging(): Boolean

    /**
     * Check whether Batter Optimizations have been disabled for the application.
     */
    fun isBatterSaverDisabled(): Boolean
}

class PowerStatusProviderImpl(
    private val powerManager: PowerManager,
    private val batteryManager: BatteryManager,
    private val packageName: String
) : PowerStatusProvider {

    override fun isCharging(): Boolean {
        return batteryManager.isCharging
    }

    override fun isBatterSaverDisabled(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }
}