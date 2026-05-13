package com.meshverse.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.meshverse.app.workers.MessageRetryWorker
import com.meshverse.app.workers.PostSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
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
        scheduleBackgroundWorkers()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /** Schedule periodic background workers for message retry and post sync. */
    private fun scheduleBackgroundWorkers() {
        val workManager = WorkManager.getInstance(this)

        // Retry pending messages every 15 minutes (minimum WorkManager interval)
        val messageRetryRequest = PeriodicWorkRequestBuilder<MessageRetryWorker>(
            15, TimeUnit.MINUTES
        ).addTag(MessageRetryWorker.WORK_NAME).build()

        workManager.enqueueUniquePeriodicWork(
            MessageRetryWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            messageRetryRequest
        )

        // Sync feed posts every 30 minutes (relaxed constraints — works offline too)
        val postSyncRequest = PeriodicWorkRequestBuilder<PostSyncWorker>(
            30, TimeUnit.MINUTES
        ).addTag(PostSyncWorker.WORK_NAME).build()

        workManager.enqueueUniquePeriodicWork(
            PostSyncWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            postSyncRequest
        )
    }

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

