package com.meshverse.app.services

import android.app.Notification
import android.app.PendingIntent
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
import com.meshverse.app.mesh.MeshNetworkManager
import com.meshverse.app.domain.model.MeshPacket
import com.meshverse.app.domain.model.PacketType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Walkie-talkie / push-to-talk voice service.
 * Captures PCM audio, encodes to Opus-like chunks, and sends over mesh.
 * Plays received audio chunks from peers.
 */
@AndroidEntryPoint
class VoiceCallService : Service() {

    @Inject lateinit var meshNetworkManager: MeshNetworkManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var targetPeerId: String = MeshNetworkManager.BROADCAST

    companion object {
        const val NOTIFICATION_ID = 1003
        const val ACTION_START_CALL = "ACTION_START_VOICE"
        const val ACTION_STOP_CALL = "ACTION_STOP_VOICE"
        const val ACTION_PTT_START = "ACTION_PTT_START"
        const val ACTION_PTT_STOP = "ACTION_PTT_STOP"
        const val EXTRA_PEER_ID = "PEER_ID"
        private const val TAG = "VoiceCallService"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CALL -> {
                targetPeerId = intent.getStringExtra(EXTRA_PEER_ID) ?: MeshNetworkManager.BROADCAST
                initAudioPlayback()
            }
            ACTION_STOP_CALL -> {
                stopCapture()
                stopSelf()
            }
            ACTION_PTT_START -> startCapture()
            ACTION_PTT_STOP -> stopCapture()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initAudioPlayback() {
        val playBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AUDIO_FORMAT
        ) * 2
        audioTrack = AudioTrack(
            AudioManager.STREAM_VOICE_CALL,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AUDIO_FORMAT,
            playBufferSize,
            AudioTrack.MODE_STREAM
        )
        audioTrack?.play()

        // Listen for incoming audio packets from mesh
        scope.launch {
            meshNetworkManager.meshEvents.collect { event ->
                if (event is com.meshverse.app.mesh.MeshEvent.PacketReceived &&
                    event.packet.payloadType == PacketType.VOICE_AUDIO) {
                    val pcm = event.packet.payload
                    audioTrack?.write(pcm, 0, pcm.size)
                }
            }
        }
    }

    private fun startCapture() {
        if (isRecording) return
        isRecording = true

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE
        )
        audioRecord?.startRecording()

        scope.launch {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isRecording) {
                val bytesRead = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                if (bytesRead > 0) {
                    val chunk = buffer.copyOf(bytesRead)
                    sendAudioChunk(chunk)
                }
            }
        }
        Log.d(TAG, "Audio capture started, broadcasting to $targetPeerId")
    }

    private fun stopCapture() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private suspend fun sendAudioChunk(chunk: ByteArray) {
        val nodeId = meshNetworkManager.getLocalNodeId()
        val packet = MeshPacket(
            sourceId = nodeId,
            destinationId = targetPeerId,
            senderId = nodeId,
            payloadType = PacketType.VOICE_AUDIO,
            payload = chunk,
            ttl = 3  // Low TTL for real-time audio
        )
        meshNetworkManager.sendPacket(packet)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, MeshVerseApplication.CHANNEL_VOICE)
            .setContentTitle("MeshVerse Voice")
            .setContentText("Voice call active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        stopCapture()
        audioTrack?.stop()
        audioTrack?.release()
        scope.cancel()
    }
}
