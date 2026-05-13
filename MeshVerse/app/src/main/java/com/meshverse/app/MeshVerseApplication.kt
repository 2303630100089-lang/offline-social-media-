package com.meshverse.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MeshVerseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        runCatching { createNotificationChannels() }
            .onFailure { throwable ->
                android.util.Log.e("MeshVerseApplication", "Unable to create notification channels", throwable)
            }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Mesh service channel
            NotificationChannel(
                CHANNEL_MESH_SERVICE,
                "Mesh Network Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the mesh network running in the background"
                notificationManager.createNotificationChannel(this)
            }

            // Messages channel
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New message notifications"
                notificationManager.createNotificationChannel(this)
            }

            // Voice calls channel
            NotificationChannel(
                CHANNEL_VOICE,
                "Voice Calls",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Incoming voice call notifications"
                notificationManager.createNotificationChannel(this)
            }

            // Emergency channel
            NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Emergency SOS and distress alerts"
                notificationManager.createNotificationChannel(this)
            }

            // Sync channel
            NotificationChannel(
                CHANNEL_SYNC,
                "Background Sync",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background data sync operations"
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    companion object {
        const val CHANNEL_MESH_SERVICE = "mesh_service"
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_VOICE = "voice_calls"
        const val CHANNEL_EMERGENCY = "emergency"
        const val CHANNEL_SYNC = "sync"
    }
}
