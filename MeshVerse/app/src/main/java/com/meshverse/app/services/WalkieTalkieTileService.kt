package com.meshverse.app.services

import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

/**
 * Quick Settings fallback trigger when hardware interception is unavailable.
 */
class WalkieTalkieTileService : TileService() {

    companion object {
        private const val PREFS_TILE = "walkie_shortcut_tile"
        private const val KEY_TILE_RECORDING = "tile_recording"
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTileState()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, WalkieTalkieShortcutService::class.java).apply {
            action = WalkieTalkieShortcutService.ACTION_TILE_TOGGLE
        }
        ContextCompat.startForegroundService(this, intent)
        qsTile?.apply {
            state = if (state == Tile.STATE_ACTIVE) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            updateTile()
        }
    }

    private fun refreshTileState() {
        val recording = getSharedPreferences(PREFS_TILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_TILE_RECORDING, false)
        qsTile?.apply {
            state = if (recording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}

