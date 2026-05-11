package com.meshverse.app.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.meshverse.app.MeshVerseApplication
import com.meshverse.app.R
import com.meshverse.app.domain.repository.UserRepository
import com.meshverse.app.mesh.MeshNetworkManager
import com.meshverse.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * Foreground service that keeps the mesh network alive.
 * Runs continuously to maintain peer connections, relay messages,
 * and process incoming mesh packets.
 */
@AndroidEntryPoint
class MeshService : Service() {

    @Inject lateinit var meshNetworkManager: MeshNetworkManager
    @Inject lateinit var userRepository: UserRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectedPeers = 0

    companion object {
        const val ACTION_START = "ACTION_START_MESH"
        const val ACTION_STOP = "ACTION_STOP_MESH"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification(0))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMeshNetwork()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMeshNetwork() {
        scope.launch {
            val localUser = userRepository.getLocalUserOnce()
            val nodeId = localUser?.userId ?: generateTemporaryNodeId()
            meshNetworkManager.initialize(nodeId)
            meshNetworkManager.startMesh()

            // Update notification as peer count changes
            meshNetworkManager.connectedPeerCount.collectLatest { count ->
                connectedPeers = count
                updateNotification(count)
            }
        }
    }

    private fun generateTemporaryNodeId(): String =
        "node_${System.currentTimeMillis()}"

    private fun buildNotification(peerCount: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MeshVerseApplication.CHANNEL_MESH_SERVICE)
            .setContentTitle(getString(R.string.mesh_service_notification_title))
            .setContentText(
                getString(R.string.mesh_service_notification_text, peerCount)
            )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(peerCount: Int) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(peerCount))
    }

    override fun onDestroy() {
        super.onDestroy()
        meshNetworkManager.stopMesh()
        scope.cancel()
    }
}
