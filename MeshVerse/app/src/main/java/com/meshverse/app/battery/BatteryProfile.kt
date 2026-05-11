package com.meshverse.app.battery

/**
 * Battery profiles controlling how aggressively the app uses hardware resources.
 *
 * Profile selection strategy:
 *  - PERFORMANCE  : battery > 50%, device not in power-save mode
 *  - BALANCED     : battery 20–50%
 *  - LOW_POWER    : battery 10–20% or power-save mode active
 *  - ULTRA_SAVING : battery < 10%
 *  - EMERGENCY    : user-activated; preserves only SOS, walkie-talkie and location sync
 */
enum class BatteryProfile {
    PERFORMANCE,
    BALANCED,
    LOW_POWER,
    ULTRA_SAVING,
    EMERGENCY;

    /** BLE scan interval in milliseconds for this profile. */
    val bleScanIntervalMs: Long get() = when (this) {
        PERFORMANCE  -> 5_000L
        BALANCED     -> 15_000L
        LOW_POWER    -> 45_000L
        ULTRA_SAVING -> 120_000L
        EMERGENCY    -> 300_000L
    }

    /** Nearby Connections discovery interval in milliseconds. */
    val nearbyDiscoveryIntervalMs: Long get() = when (this) {
        PERFORMANCE  -> 0L         // Continuous
        BALANCED     -> 30_000L
        LOW_POWER    -> 90_000L
        ULTRA_SAVING -> 300_000L
        EMERGENCY    -> 600_000L
    }

    /** Background gossip synchronization period. */
    val gossipSyncIntervalMs: Long get() = when (this) {
        PERFORMANCE  -> 10_000L
        BALANCED     -> 60_000L
        LOW_POWER    -> 300_000L
        ULTRA_SAVING -> 600_000L
        EMERGENCY    -> Long.MAX_VALUE  // Disabled – sync only on peer connect
    }

    /** GPS location update interval in milliseconds. */
    val locationUpdateIntervalMs: Long get() = when (this) {
        PERFORMANCE  -> 5_000L
        BALANCED     -> 30_000L
        LOW_POWER    -> 120_000L
        ULTRA_SAVING -> 300_000L
        EMERGENCY    -> 600_000L
    }

    /** Maximum media propagation chunk size. 0 = media propagation disabled. */
    val maxMediaChunkBytes: Int get() = when (this) {
        PERFORMANCE  -> 512 * 1024   // 512 KB
        BALANCED     -> 128 * 1024   // 128 KB
        LOW_POWER    -> 32  * 1024   // 32 KB
        ULTRA_SAVING -> 0
        EMERGENCY    -> 0
    }

    /** Whether heavy AI inference is allowed. */
    val aiEnabled: Boolean get() = this == PERFORMANCE || this == BALANCED

    companion object {
        /**
         * Determine the appropriate profile from current device state.
         *
         * @param batteryPct     Current battery percentage (0–100).
         * @param isCharging     True if device is currently charging.
         * @param isPowerSaveOn  True if Android battery-saver mode is active.
         * @param thermalState   Android thermal status (0 = none, 1 = light, 2 = moderate, 3+= severe)
         */
        fun from(
            batteryPct: Int,
            isCharging: Boolean,
            isPowerSaveOn: Boolean,
            thermalState: Int = 0
        ): BatteryProfile {
            if (isCharging && thermalState < 2) return PERFORMANCE
            if (isPowerSaveOn)                  return LOW_POWER
            if (thermalState >= 3)              return LOW_POWER
            return when {
                batteryPct >  50 -> PERFORMANCE
                batteryPct >  20 -> BALANCED
                batteryPct >  10 -> LOW_POWER
                else             -> ULTRA_SAVING
            }
        }
    }
}
