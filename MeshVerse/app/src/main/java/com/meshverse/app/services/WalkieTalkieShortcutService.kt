package com.meshverse.app.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meshverse.app.MeshVerseApplication
import com.meshverse.app.R
import com.meshverse.app.domain.model.MeshPacket
import com.meshverse.app.domain.model.PacketType
import com.meshverse.app.domain.repository.PeerRepository
import com.meshverse.app.domain.repository.UserRepository
import com.meshverse.app.domain.repository.WalkieTalkieRepository
import com.meshverse.app.mesh.MeshEvent
import com.meshverse.app.mesh.MeshNetworkManager
import com.meshverse.app.security.CryptoManager
import com.meshverse.app.security.KeyManager
import com.meshverse.app.ui.MainActivity
import com.meshverse.app.walkie.WalkieTalkieManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * Persistent foreground service for system-level walkie-talkie shortcut behavior.
 *
 * Security notes:
 * - Audio payload is encrypted end-to-end per recipient (ephemeral message key wrapped with ECDH).
 * - Relay nodes only forward encrypted envelopes and cannot decrypt voice data.
 *
 * Battery notes:
 * - Service stays foreground and only captures microphone after a confirmed long-press.
 * - Retry loop uses coarse periodic checks and bounded queue size to minimize wakeups.
 *
 * Interception notes:
 * - Uses MediaSession for media key handling and AccessibilityService for hardware key filtering.
 * - Android may block power key interception on many devices; fallback triggers are provided.
 */
@AndroidEntryPoint
class WalkieTalkieShortcutService : Service() {

    @Inject lateinit var meshNetworkManager: MeshNetworkManager
    @Inject lateinit var walkieTalkieManager: WalkieTalkieManager
    @Inject lateinit var walkieTalkieRepository: WalkieTalkieRepository
    @Inject lateinit var peerRepository: PeerRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var keyManager: KeyManager
    @Inject lateinit var cryptoManager: CryptoManager
    @Inject lateinit var gson: Gson

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var mediaSession: MediaSession? = null
    private var overlayController: WalkieOverlayController? = null

    private var longPressJob: Job? = null
    private var queueFlushJob: Job? = null
    private var recordJob: Job? = null

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var selectedRoomId: String? = null
    private var recordingStream: ByteArrayOutputStream? = null
    private var lastMediaTransportActionAt = 0L

    private val queuePrefs by lazy { getSharedPreferences(PREFS_QUEUE, Context.MODE_PRIVATE) }
    private val tilePrefs by lazy { getSharedPreferences(PREFS_TILE, Context.MODE_PRIVATE) }

    companion object {
        private const val TAG = "WalkieShortcutService"

        const val NOTIFICATION_ID = 2011

        const val ACTION_START_LISTENING = "walkie.action.START_LISTENING"
        const val ACTION_BUTTON_DOWN = "walkie.action.BUTTON_DOWN"
        const val ACTION_BUTTON_UP = "walkie.action.BUTTON_UP"
        const val ACTION_TILE_TOGGLE = "walkie.action.TILE_TOGGLE"
        const val ACTION_SET_ROOM = "walkie.action.SET_ROOM"

        const val EXTRA_ROOM_ID = "walkie.extra.ROOM_ID"

        private const val LONG_PRESS_MS = 380L
        private const val MAX_RECORD_MS = 30_000L

        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AUDIO_FORMAT) * 2

        private const val AUDIO_GCM_IV_LENGTH = 12
        private const val AUDIO_GCM_TAG_LENGTH = 128
        private const val AUDIO_SESSION_KEY_BYTES = 32

        private const val PREFS_QUEUE = "walkie_shortcut_queue"
        private const val PREFS_TILE = "walkie_shortcut_tile"
        private const val KEY_QUEUE = "queue_items"
        private const val KEY_TILE_RECORDING = "tile_recording"
        private const val MAX_QUEUE_ITEMS = 120
        private const val MAX_RETRIES = 8
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.walkie_shortcut_notification_text)))

        walkieTalkieManager.start()
        initMediaSession()
        initOverlay()
        observeIncomingPackets()
        startQueueRetryLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> Unit
            ACTION_BUTTON_DOWN -> onButtonPressed()
            ACTION_BUTTON_UP -> onButtonReleased()
            ACTION_TILE_TOGGLE -> onTileToggle()
            ACTION_SET_ROOM -> selectedRoomId = intent.getStringExtra(EXTRA_ROOM_ID)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "meshverse-walkie-shortcut").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(
                PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY_PAUSE)
                    .setState(PlaybackState.STATE_PLAYING, 0L, 1f)
                    .build()
            )
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    if (shouldHandleMediaTransportAction()) onButtonPressed()
                }

                override fun onPause() {
                    if (shouldHandleMediaTransportAction()) onButtonReleased()
                }
            })
            isActive = true
        }
    }

    private fun shouldHandleMediaTransportAction(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastMediaTransportActionAt < 150L) return false
        lastMediaTransportActionAt = now
        return true
    }

    private fun initOverlay() {
        overlayController = WalkieOverlayController(
            context = this,
            onPressDown = { onButtonPressed() },
            onPressUp = { onButtonReleased() }
        )
        overlayController?.showIdleBubble()
    }

    private fun onButtonPressed() {
        if (isRecording) return
        longPressJob?.cancel()
        longPressJob = scope.launch {
            delay(LONG_PRESS_MS)
            beginRecording()
        }
    }

    private fun onButtonReleased() {
        longPressJob?.cancel()
        if (isRecording) {
            scope.launch { finishRecordingAndDispatch() }
        }
    }

    private fun onTileToggle() {
        if (isRecording) {
            onButtonReleased()
        } else {
            onButtonPressed()
        }
    }

    private suspend fun beginRecording() {
        if (isRecording) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Microphone permission not granted")
            return
        }

        val roomId = resolveTargetRoomId()
        val localId = resolveLocalId()
        walkieTalkieManager.startPtt(roomId, localId)

        isRecording = true
        setTileRecordingState(true)
        overlayController?.showRecordingState()
        updateNotification(getString(R.string.walkie_recording_notification_text))

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_IN,
            AUDIO_FORMAT,
            BUFFER_SIZE
        )
        audioRecord = recorder
        recorder.startRecording()

        val pcmStream = ByteArrayOutputStream()
        recordingStream = pcmStream
        val startMs = System.currentTimeMillis()

        recordJob?.cancel()
        recordJob = scope.launch {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isRecording) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    pcmStream.write(buffer, 0, read)
                    overlayController?.updateWaveAmplitude(buffer, read)
                }
                if (System.currentTimeMillis() - startMs >= MAX_RECORD_MS) {
                    onButtonReleased()
                    break
                }
            }
        }
    }

    private suspend fun finishRecordingAndDispatch() {
        if (!isRecording) return

        isRecording = false
        setTileRecordingState(false)

        val recorder = audioRecord
        audioRecord = null

        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }

        recordJob?.cancel()

        val roomId = resolveTargetRoomId()
        val localId = resolveLocalId()
        walkieTalkieManager.stopPtt(roomId, localId)

        overlayController?.showIdleState()
        updateNotification(getString(R.string.walkie_shortcut_notification_text))

        val pcmBytes = recordingStream?.toByteArray() ?: ByteArray(0)
        recordingStream = null
        if (pcmBytes.isEmpty()) return

        sendOrQueueGroupVoice(roomId = roomId, senderId = localId, audioPcm = pcmBytes)
    }

    private suspend fun sendOrQueueGroupVoice(roomId: String, senderId: String, audioPcm: ByteArray) {
        val room = walkieTalkieRepository.getRoomById(roomId) ?: walkieTalkieRepository.getEmergencyChannel()
        val connectedPeers = peerRepository.getConnectedPeersOnce()
        val connectedById = connectedPeers.associateBy { it.peerId }

        val recipients = ((room?.memberIds ?: emptyList()) + connectedById.keys)
            .filter { it != senderId }
            .distinct()

        recipients.forEach { recipientId ->
            val peer = connectedById[recipientId]
            if (peer?.publicKey.isNullOrBlank()) {
                enqueueVoice(
                    QueuedVoiceEnvelope(
                        queueId = UUID.randomUUID().toString(),
                        roomId = roomId,
                        senderId = senderId,
                        recipientId = recipientId,
                        audioPcmBase64 = Base64.encodeToString(audioPcm, Base64.NO_WRAP)
                    )
                )
                return@forEach
            }

            if (peer?.isConnected != true) {
                enqueueVoice(
                    QueuedVoiceEnvelope(
                        queueId = UUID.randomUUID().toString(),
                        roomId = roomId,
                        senderId = senderId,
                        recipientId = recipientId,
                        audioPcmBase64 = Base64.encodeToString(audioPcm, Base64.NO_WRAP)
                    )
                )
                return@forEach
            }

            runCatching {
                sendEncryptedVoiceEnvelope(
                    roomId = roomId,
                    senderId = senderId,
                    recipientId = recipientId,
                    recipientPublicKey = peer.publicKey!!,
                    audioPcm = audioPcm,
                    queued = false
                )
            }.onFailure {
                enqueueVoice(
                    QueuedVoiceEnvelope(
                        queueId = UUID.randomUUID().toString(),
                        roomId = roomId,
                        senderId = senderId,
                        recipientId = recipientId,
                        audioPcmBase64 = Base64.encodeToString(audioPcm, Base64.NO_WRAP)
                    )
                )
            }
        }
    }

    private suspend fun sendEncryptedVoiceEnvelope(
        roomId: String,
        senderId: String,
        recipientId: String,
        recipientPublicKey: String,
        audioPcm: ByteArray,
        queued: Boolean
    ) {
        val sessionKey = ByteArray(AUDIO_SESSION_KEY_BYTES).also { SecureRandom().nextBytes(it) }
        val wrappedSessionKey = cryptoManager.encrypt(
            Base64.encodeToString(sessionKey, Base64.NO_WRAP),
            recipientPublicKey
        )

        val (iv, ciphertext) = encryptAudioWithSessionKey(audioPcm, sessionKey)
        val envelope = WalkieAudioEnvelope(
            roomId = roomId,
            senderId = senderId,
            recipientId = recipientId,
            senderPublicKey = keyManager.getPublicKeyBase64(),
            wrappedSessionKey = wrappedSessionKey,
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            cipherBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            queuedDelivery = queued,
            timestamp = System.currentTimeMillis()
        )

        val packet = MeshPacket(
            sourceId = meshNetworkManager.getLocalNodeId().ifBlank { senderId },
            destinationId = recipientId,
            senderId = meshNetworkManager.getLocalNodeId().ifBlank { senderId },
            payloadType = PacketType.WALKIE_AUDIO,
            payload = gson.toJson(envelope).toByteArray(Charsets.UTF_8),
            ttl = 5
        )
        meshNetworkManager.sendPacket(packet)
    }

    private fun encryptAudioWithSessionKey(audioPcm: ByteArray, sessionKey: ByteArray): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(AUDIO_GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(sessionKey, "AES"),
            GCMParameterSpec(AUDIO_GCM_TAG_LENGTH, iv)
        )
        return iv to cipher.doFinal(audioPcm)
    }

    private fun decryptAudioWithSessionKey(ciphertext: ByteArray, sessionKey: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(sessionKey, "AES"),
            GCMParameterSpec(AUDIO_GCM_TAG_LENGTH, iv)
        )
        return cipher.doFinal(ciphertext)
    }

    private fun observeIncomingPackets() {
        scope.launch {
            meshNetworkManager.meshEvents.collectLatest { event ->
                if (event is MeshEvent.PacketReceived && event.packet.payloadType == PacketType.WALKIE_AUDIO) {
                    handleIncomingWalkieAudio(event.packet)
                }
            }
        }
    }

    private suspend fun handleIncomingWalkieAudio(packet: MeshPacket) {
        runCatching {
            val envelope = gson.fromJson(String(packet.payload, Charsets.UTF_8), WalkieAudioEnvelope::class.java)
            if (envelope.recipientId != resolveLocalId()) return@runCatching

            val sessionKeyB64 = cryptoManager.decrypt(
                envelope.wrappedSessionKey,
                envelope.senderPublicKey
            )
            val sessionKey = Base64.decode(sessionKeyB64, Base64.NO_WRAP)
            val iv = Base64.decode(envelope.ivBase64, Base64.NO_WRAP)
            val ciphertext = Base64.decode(envelope.cipherBase64, Base64.NO_WRAP)
            val pcm = decryptAudioWithSessionKey(ciphertext, sessionKey, iv)

            overlayController?.flashIncomingState()
            playIncomingPcm(pcm)
        }.onFailure { throwable ->
            Log.e(TAG, "Unable to decrypt incoming walkie audio", throwable)
        }
    }

    private fun playIncomingPcm(pcm: ByteArray) {
        scope.launch {
            val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, AUDIO_FORMAT)
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                CHANNEL_OUT,
                AUDIO_FORMAT,
                minBuffer.coerceAtLeast(pcm.size),
                AudioTrack.MODE_STREAM
            )
            runCatching {
                track.play()
                track.write(pcm, 0, pcm.size)
                track.stop()
                track.release()
            }.onFailure {
                runCatching { track.release() }
            }
        }
    }

    private fun startQueueRetryLoop() {
        queueFlushJob?.cancel()
        queueFlushJob = scope.launch {
            while (true) {
                delay(20_000)
                flushQueue()
            }
        }
    }

    private suspend fun flushQueue() {
        val queue = loadQueue().toMutableList()
        if (queue.isEmpty()) return

        val connectedPeers = peerRepository.getConnectedPeersOnce().associateBy { it.peerId }
        val retained = mutableListOf<QueuedVoiceEnvelope>()

        queue.forEach { item ->
            val peer = connectedPeers[item.recipientId]
            if (peer?.isConnected != true || peer.publicKey.isNullOrBlank()) {
                retained += item
                return@forEach
            }

            val audio = runCatching { Base64.decode(item.audioPcmBase64, Base64.NO_WRAP) }.getOrNull()
            if (audio == null || audio.isEmpty()) return@forEach

            val sent = runCatching {
                sendEncryptedVoiceEnvelope(
                    roomId = item.roomId,
                    senderId = item.senderId,
                    recipientId = item.recipientId,
                    recipientPublicKey = peer.publicKey!!,
                    audioPcm = audio,
                    queued = true
                )
                true
            }.getOrElse { false }

            if (!sent) {
                val nextRetry = item.retryCount + 1
                if (nextRetry <= MAX_RETRIES) {
                    retained += item.copy(retryCount = nextRetry)
                }
            }
        }

        saveQueue(retained.takeLast(MAX_QUEUE_ITEMS))
    }

    private fun enqueueVoice(item: QueuedVoiceEnvelope) {
        val queue = loadQueue().toMutableList()
        queue += item
        saveQueue(queue.takeLast(MAX_QUEUE_ITEMS))
    }

    private fun loadQueue(): List<QueuedVoiceEnvelope> {
        val raw = queuePrefs.getString(KEY_QUEUE, null) ?: return emptyList()
        val type = object : TypeToken<List<QueuedVoiceEnvelope>>() {}.type
        return runCatching { gson.fromJson<List<QueuedVoiceEnvelope>>(raw, type) ?: emptyList() }
            .getOrDefault(emptyList())
    }

    private fun saveQueue(queue: List<QueuedVoiceEnvelope>) {
        queuePrefs.edit().putString(KEY_QUEUE, gson.toJson(queue)).apply()
    }

    private suspend fun resolveTargetRoomId(): String {
        selectedRoomId?.let { return it }
        val emergency = walkieTalkieRepository.getEmergencyChannel()
        if (emergency != null) return emergency.roomId

        val fallbackRoom = walkieTalkieManager.createRoom(
            name = "Family",
            channelType = com.meshverse.app.domain.model.ChannelType.PRIVATE,
            channelNumber = 1,
            creatorId = resolveLocalId(),
            encrypted = true
        )
        selectedRoomId = fallbackRoom.roomId
        return fallbackRoom.roomId
    }

    private suspend fun resolveLocalId(): String {
        val userId = userRepository.getLocalUserOnce()?.userId
        return userId ?: meshNetworkManager.getLocalNodeId().ifBlank { "anon-${UUID.randomUUID()}" }
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MeshVerseApplication.CHANNEL_VOICE)
            .setContentTitle(getString(R.string.walkie_shortcut_notification_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun setTileRecordingState(recording: Boolean) {
        tilePrefs.edit().putBoolean(KEY_TILE_RECORDING, recording).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        walkieTalkieManager.stop()
        runCatching { mediaSession?.isActive = false }
        runCatching { mediaSession?.release() }
        mediaSession = null

        longPressJob?.cancel()
        recordJob?.cancel()
        queueFlushJob?.cancel()

        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        isRecording = false
        setTileRecordingState(false)

        overlayController?.destroy()
        overlayController = null

        scope.cancel()
    }
}

private data class WalkieAudioEnvelope(
    val roomId: String,
    val senderId: String,
    val recipientId: String,
    val senderPublicKey: String,
    val wrappedSessionKey: String,
    val ivBase64: String,
    val cipherBase64: String,
    val queuedDelivery: Boolean,
    val timestamp: Long
)

private data class QueuedVoiceEnvelope(
    val queueId: String,
    val roomId: String,
    val senderId: String,
    val recipientId: String,
    val audioPcmBase64: String,
    val retryCount: Int = 0
)

private class WalkieOverlayController(
    private val context: Context,
    private val onPressDown: () -> Unit,
    private val onPressUp: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: PttOverlayView? = null
    private var attached = false

    fun showIdleBubble() {
        if (!canDrawOverlays()) return
        if (attached) return

        val view = PttOverlayView(context)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = 28
            y = 0
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        onPressDown()
                        view.setRecording(true)
                        return true
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        onPressUp()
                        view.setRecording(false)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(view, params)
        overlayView = view
        attached = true
        view.setIdle()
    }

    fun showRecordingState() {
        overlayView?.setRecording(true)
    }

    fun showIdleState() {
        overlayView?.setIdle()
    }

    fun flashIncomingState() {
        overlayView?.flashIncoming()
    }

    fun updateWaveAmplitude(buffer: ByteArray, length: Int) {
        var max = 0
        var i = 0
        while (i < length - 1) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xff))
            val abs = kotlin.math.abs(sample)
            if (abs > max) max = abs
            i += 2
        }
        overlayView?.setAmplitude(max / 32767f)
    }

    fun destroy() {
        if (attached) {
            runCatching { windowManager.removeViewImmediate(overlayView) }
            attached = false
            overlayView = null
        }
    }

    private fun canDrawOverlays(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }
}

private class PttOverlayView(context: Context) : View(context) {
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2200F5FF")
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA00F5FF")
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF7B2FFF")
        style = Paint.Style.FILL
    }

    private var pulse = 0f
    private var amplitude = 0.1f
    private var recording = false
    private var incomingPulse = 0f

    init {
        layoutParams = WindowManager.LayoutParams(170, 170)
        post(object : Runnable {
            override fun run() {
                pulse += if (recording) 0.18f else 0.08f
                incomingPulse = (incomingPulse - 0.05f).coerceAtLeast(0f)
                invalidate()
                postDelayed(this, 16)
            }
        })
    }

    fun setRecording(active: Boolean) {
        recording = active
        invalidate()
    }

    fun setIdle() {
        recording = false
        amplitude = 0.1f
        invalidate()
    }

    fun flashIncoming() {
        incomingPulse = 1f
        invalidate()
    }

    fun setAmplitude(value: Float) {
        amplitude = value.coerceIn(0.05f, 1f)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(170, 170)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = 48f
        val pulseRadius = baseRadius + (kotlin.math.sin(pulse.toDouble()).toFloat() + 1f) * if (recording) 16f else 6f

        basePaint.color = if (recording) Color.parseColor("#3300F5FF") else Color.parseColor("#2200F5FF")
        canvas.drawCircle(cx, cy, pulseRadius, basePaint)

        glowPaint.alpha = if (recording) 255 else 140
        canvas.drawCircle(cx, cy, baseRadius + 10f, glowPaint)

        val waveHeight = 54f * amplitude
        val barWidth = 10f
        val gap = 7f
        for (idx in -3..3) {
            val barCenterX = cx + idx * (barWidth + gap)
            val factor = 1f - (kotlin.math.abs(idx) / 4f)
            val halfH = waveHeight * factor
            canvas.drawRoundRect(
                barCenterX - barWidth / 2f,
                cy - halfH,
                barCenterX + barWidth / 2f,
                cy + halfH,
                8f,
                8f,
                wavePaint
            )
        }

        if (incomingPulse > 0f) {
            val incomingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 8f
                color = Color.parseColor("#FFFF006E")
                alpha = (incomingPulse * 255).toInt()
            }
            canvas.drawCircle(cx, cy, baseRadius + 26f + (1f - incomingPulse) * 20f, incomingPaint)
        }
    }
}
