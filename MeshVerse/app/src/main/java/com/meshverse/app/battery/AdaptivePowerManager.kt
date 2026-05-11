package com.meshverse.app.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors battery state and exposes the current [BatteryProfile].
 *
 * Other components observe [currentProfile] and adjust their behaviour
 * (scan intervals, sync frequency, media propagation, AI usage) accordingly.
 *
 * Architecture note:
 *  - BroadcastReceiver listens for battery change intents (no polling overhead).
 *  - PowerManager query detects battery-saver mode.
 *  - Thermal status is polled on API 29+ from [PowerManager.getCurrentThermalStatus].
 */
@Singleton
class AdaptivePowerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AdaptivePowerManager"
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val _currentProfile = MutableStateFlow(BatteryProfile.BALANCED)
    /** Observe this to react to battery profile changes across the app. */
    val currentProfile: StateFlow<BatteryProfile> = _currentProfile

    private var isRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                updateProfile(intent)
            }
        }
    }

    /** Register the battery broadcast receiver. Call from Application.onCreate or a service. */
    fun start() {
        if (isRegistered) return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
        isRegistered = true

        // Initialise immediately with current state
        val stickyIntent = context.registerReceiver(null, filter)
        stickyIntent?.let { updateProfile(it) }

        Log.d(TAG, "AdaptivePowerManager started, profile=${_currentProfile.value}")
    }

    fun stop() {
        if (!isRegistered) return
        context.unregisterReceiver(batteryReceiver)
        isRegistered = false
    }

    /**
     * Allow external components (e.g. emergency mode button) to force a profile.
     * The forced profile is overridden on the next battery event.
     */
    fun forceProfile(profile: BatteryProfile) {
        Log.i(TAG, "Profile manually forced to $profile")
        _currentProfile.value = profile
    }

    private fun updateProfile(intent: Intent) {
        val level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status  = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val pct     = if (scale > 0) (level * 100 / scale) else 50
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                       status == BatteryManager.BATTERY_STATUS_FULL

        val isPowerSave = powerManager.isPowerSaveMode

        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.currentThermalStatus
        } else 0

        val newProfile = BatteryProfile.from(pct, charging, isPowerSave, thermal)
        if (newProfile != _currentProfile.value) {
            Log.i(TAG, "Battery profile changed: ${_currentProfile.value} → $newProfile (bat=$pct%, charging=$charging)")
            _currentProfile.value = newProfile
        }
    }
}
