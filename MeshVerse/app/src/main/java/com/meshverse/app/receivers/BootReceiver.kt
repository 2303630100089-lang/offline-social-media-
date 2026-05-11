package com.meshverse.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meshverse.app.services.MeshService
import com.meshverse.app.services.SyncService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // Auto-start mesh service on boot
            context.startForegroundService(
                Intent(context, MeshService::class.java).apply {
                    action = MeshService.ACTION_START
                }
            )
            context.startForegroundService(
                Intent(context, SyncService::class.java)
            )
        }
    }
}
