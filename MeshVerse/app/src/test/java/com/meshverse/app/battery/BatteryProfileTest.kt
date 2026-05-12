package com.meshverse.app.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryProfileTest {

    @Test
    fun `from returns performance when charging and thermal is safe`() {
        val profile = BatteryProfile.from(
            batteryPct = 5,
            isCharging = true,
            isPowerSaveOn = false,
            thermalState = 1
        )

        assertEquals(BatteryProfile.PERFORMANCE, profile)
    }

    @Test
    fun `from returns low power when power save is enabled`() {
        val profile = BatteryProfile.from(
            batteryPct = 80,
            isCharging = false,
            isPowerSaveOn = true,
            thermalState = 0
        )

        assertEquals(BatteryProfile.LOW_POWER, profile)
    }

    @Test
    fun `from returns low power when thermal state is severe`() {
        val profile = BatteryProfile.from(
            batteryPct = 80,
            isCharging = false,
            isPowerSaveOn = false,
            thermalState = 3
        )

        assertEquals(BatteryProfile.LOW_POWER, profile)
    }

    @Test
    fun `from maps battery thresholds correctly`() {
        assertEquals(BatteryProfile.PERFORMANCE, BatteryProfile.from(51, false, false))
        assertEquals(BatteryProfile.BALANCED, BatteryProfile.from(50, false, false))
        assertEquals(BatteryProfile.BALANCED, BatteryProfile.from(21, false, false))
        assertEquals(BatteryProfile.LOW_POWER, BatteryProfile.from(20, false, false))
        assertEquals(BatteryProfile.LOW_POWER, BatteryProfile.from(11, false, false))
        assertEquals(BatteryProfile.ULTRA_SAVING, BatteryProfile.from(10, false, false))
        assertEquals(BatteryProfile.ULTRA_SAVING, BatteryProfile.from(0, false, false))
    }

    @Test
    fun `profile capabilities match expected behavior`() {
        assertTrue(BatteryProfile.PERFORMANCE.aiEnabled)
        assertTrue(BatteryProfile.BALANCED.aiEnabled)
        assertFalse(BatteryProfile.LOW_POWER.aiEnabled)
        assertFalse(BatteryProfile.ULTRA_SAVING.aiEnabled)
        assertFalse(BatteryProfile.EMERGENCY.aiEnabled)

        assertEquals(0, BatteryProfile.ULTRA_SAVING.maxMediaChunkBytes)
        assertEquals(0, BatteryProfile.EMERGENCY.maxMediaChunkBytes)
        assertEquals(Long.MAX_VALUE, BatteryProfile.EMERGENCY.gossipSyncIntervalMs)
    }
}
