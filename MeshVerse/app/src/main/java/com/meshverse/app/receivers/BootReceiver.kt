package com.meshverse.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.meshverse.app.services.MeshService
import com.meshverse.app.services.SyncService

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // Auto-start mesh service on boot
            runCatching {
                context.startForegroundService(
                    Intent(context, MeshService::class.java).apply {
                        action = MeshService.ACTION_START
                    }
                )
            }.onFailure { throwable ->
                Log.e(TAG, "Unable to start mesh service after boot", throwable)
            }
            runCatching {
                context.startForegroundService(
                    Intent(context, SyncService::class.java)
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Unable to start sync service after boot", throwable)
            }
        }
    }
}
