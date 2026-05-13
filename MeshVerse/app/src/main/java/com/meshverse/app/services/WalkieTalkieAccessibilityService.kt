package com.meshverse.app.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

/**
 * Hardware-key fallback trigger.
 *
 * Android often blocks direct power-key interception for third-party apps.
 * This service requests key filtering and attempts to detect long-press intent;
 * volume-key fallback remains the primary reliable path on most OEM builds.
 */
class WalkieTalkieAccessibilityService : AccessibilityService() {

    private var volumeDownPressed = false
    private var volumeUpPressed = false
    private var comboActive = false
    private var lastVolumeDownAt = 0L
    private var lastVolumeUpAt = 0L

    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return true
        when (event.keyCode) {
            KeyEvent.KEYCODE_POWER -> handlePowerKey(event)
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP -> handleVolumeKey(event)
        }
        return false
    }

    private fun handlePowerKey(event: KeyEvent) {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> sendTrigger(WalkieTalkieShortcutService.ACTION_BUTTON_DOWN)
            KeyEvent.ACTION_UP -> sendTrigger(WalkieTalkieShortcutService.ACTION_BUTTON_UP)
        }
    }

    private fun handleVolumeKey(event: KeyEvent) {
        val now = System.currentTimeMillis()
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volumeDownPressed = event.action == KeyEvent.ACTION_DOWN
                if (event.action == KeyEvent.ACTION_DOWN) lastVolumeDownAt = now
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volumeUpPressed = event.action == KeyEvent.ACTION_DOWN
                if (event.action == KeyEvent.ACTION_DOWN) lastVolumeUpAt = now
            }
        }

        if (!comboActive &&
            volumeDownPressed &&
            volumeUpPressed &&
            kotlin.math.abs(lastVolumeDownAt - lastVolumeUpAt) <= 320L) {
            comboActive = true
            sendTrigger(WalkieTalkieShortcutService.ACTION_BUTTON_DOWN)
            return
        }

        if (comboActive && (!volumeDownPressed || !volumeUpPressed)) {
            comboActive = false
            sendTrigger(WalkieTalkieShortcutService.ACTION_BUTTON_UP)
            return
        }

        if (!comboActive) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> sendTrigger(WalkieTalkieShortcutService.ACTION_BUTTON_DOWN)
                KeyEvent.ACTION_UP -> sendTrigger(WalkieTalkieShortcutService.ACTION_BUTTON_UP)
            }
        }
    }

    private fun sendTrigger(action: String) {
        val intent = Intent(this, WalkieTalkieShortcutService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(this, intent)
    }
}

