package com.meshverse.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.meshverse.app.workers.SecurityMaintenanceWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class MeshVerseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleSecurityMaintenance()
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

    private fun scheduleSecurityMaintenance() {
        val request = PeriodicWorkRequestBuilder<SecurityMaintenanceWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "security-maintenance",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
