package com.meshverse.app.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.meshverse.app.MeshVerseApplication
import com.meshverse.app.domain.model.MeshPacket
import com.meshverse.app.domain.model.PacketType
import com.meshverse.app.mesh.MeshEvent
import com.meshverse.app.mesh.MeshNetworkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Live Audio Broadcast Service – Phase 2
 *
 * Enables one-to-many live audio streaming over the mesh network.
 * Differs from VoiceCallService (walkie-talkie PTT) in that:
 *  - The broadcaster continuously captures and streams audio without push-to-talk.
 *  - All peers in range automatically receive and play the broadcast.
 *  - Supports community radio, DJ streaming, and live announcements.
 *  - Audio is relayed hop-by-hop for extended mesh coverage.
 */
@AndroidEntryPoint
class LiveAudioBroadcastService : Service() {

    @Inject lateinit var meshNetworkManager: MeshNetworkManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isBroadcasting = false
    private var broadcastChannelId: String = "community-radio"

    companion object {
        const val NOTIFICATION_ID = 2001
        const val ACTION_START_BROADCAST = "ACTION_START_BROADCAST"
        const val ACTION_STOP_BROADCAST = "ACTION_STOP_BROADCAST"
        const val ACTION_START_LISTEN = "ACTION_START_LISTEN"
        const val ACTION_STOP_LISTEN = "ACTION_STOP_LISTEN"
        const val EXTRA_CHANNEL_ID = "CHANNEL_ID"

        private const val TAG = "LiveAudioBroadcast"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AUDIO_FORMAT) * 2
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification("Live Audio Ready"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        broadcastChannelId = intent?.getStringExtra(EXTRA_CHANNEL_ID) ?: "community-radio"
        when (intent?.action) {
            ACTION_START_BROADCAST -> startBroadcast()
            ACTION_STOP_BROADCAST -> stopBroadcast()
            ACTION_START_LISTEN -> startListening()
            ACTION_STOP_LISTEN -> {
                stopListening()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Broadcaster side ───────────────────────────────────────────────────

    private fun startBroadcast() {
        if (isBroadcasting) return
        isBroadcasting = true

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_IN,
            AUDIO_FORMAT,
            BUFFER_SIZE
        )
        audioRecord?.startRecording()

        updateNotification("🎙 Broadcasting: $broadcastChannelId")
        Log.d(TAG, "Live broadcast started on channel: $broadcastChannelId")

        scope.launch {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isBroadcasting) {
                val bytesRead = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                when {
                    bytesRead > 0 -> sendBroadcastChunk(buffer.copyOf(bytesRead))
                    bytesRead < 0 -> Log.e(TAG, "AudioRecord read error: $bytesRead")
                }
            }
        }
    }

    private fun stopBroadcast() {
        isBroadcasting = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d(TAG, "Live broadcast stopped")
    }

    private suspend fun sendBroadcastChunk(chunk: ByteArray) {
        val nodeId = meshNetworkManager.getLocalNodeId()
        // Embed channel ID as a prefix in the payload: "[channelId]\n" + raw PCM
        val channelPrefix = "$broadcastChannelId\n".toByteArray(Charsets.UTF_8)
        val payload = channelPrefix + chunk
        val packet = MeshPacket(
            sourceId = nodeId,
            destinationId = MeshNetworkManager.BROADCAST,
            senderId = nodeId,
            payloadType = PacketType.VOICE_AUDIO,
            payload = payload,
            ttl = 5
        )
        meshNetworkManager.sendPacket(packet)
    }

    // ── Listener side ──────────────────────────────────────────────────────

    private fun startListening() {
        val playBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, AUDIO_FORMAT) * 2
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            CHANNEL_OUT,
            AUDIO_FORMAT,
            playBufferSize,
            AudioTrack.MODE_STREAM
        )
        audioTrack?.play()

        updateNotification("📻 Listening: $broadcastChannelId")
        Log.d(TAG, "Listening to broadcast channel: $broadcastChannelId")

        scope.launch {
            meshNetworkManager.meshEvents.collect { event ->
                if (event is MeshEvent.PacketReceived &&
                    event.packet.payloadType == PacketType.VOICE_AUDIO) {
                    handleIncomingBroadcast(event.packet.payload)
                }
            }
        }
    }

    private fun handleIncomingBroadcast(payload: ByteArray) {
        // Parse channel prefix: "[channelId]\n" + PCM data
        val newline = payload.indexOf('\n'.toByte())
        if (newline < 0) return
        val incomingChannel = String(payload, 0, newline, Charsets.UTF_8)
        if (incomingChannel != broadcastChannelId) return
        val pcm = payload.copyOfRange(newline + 1, payload.size)
        if (pcm.isNotEmpty()) {
            audioTrack?.write(pcm, 0, pcm.size)
        }
    }

    private fun stopListening() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    // ── Notification ───────────────────────────────────────────────────────

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, MeshVerseApplication.CHANNEL_VOICE)
            .setContentTitle("MeshVerse Live Audio")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBroadcast()
        stopListening()
        scope.cancel()
    }
}
