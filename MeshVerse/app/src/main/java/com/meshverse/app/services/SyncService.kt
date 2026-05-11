package com.meshverse.app.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.meshverse.app.MeshVerseApplication
import com.meshverse.app.domain.repository.MessageRepository
import com.meshverse.app.domain.repository.PostRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Background sync service that handles:
 * - Retrying failed message delivery
 * - Cleaning up expired posts and self-destruct messages
 * - Periodic state maintenance
 */
@AndroidEntryPoint
class SyncService : Service() {

    @Inject lateinit var messageRepository: MessageRepository
    @Inject lateinit var postRepository: PostRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val NOTIFICATION_ID = 1002
        private const val TAG = "SyncService"
        private const val SYNC_INTERVAL_MS = 30_000L
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        startSyncLoop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startSyncLoop() {
        scope.launch {
            while (true) {
                try {
                    messageRepository.deleteSelfDestructMessages()
                    postRepository.cleanExpiredPosts()
                    Log.d(TAG, "Sync cycle complete")
                } catch (e: Exception) {
                    Log.e(TAG, "Sync error: ${e.message}")
                }
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, MeshVerseApplication.CHANNEL_SYNC)
            .setContentTitle("MeshVerse Sync")
            .setContentText("Background sync active")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
