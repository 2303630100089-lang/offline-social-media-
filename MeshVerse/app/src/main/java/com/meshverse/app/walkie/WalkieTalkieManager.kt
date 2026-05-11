package com.meshverse.app.walkie

import android.util.Log
import com.google.gson.Gson
import com.meshverse.app.domain.model.ChannelType
import com.meshverse.app.domain.model.MeshPacket
import com.meshverse.app.domain.model.PacketType
import com.meshverse.app.domain.model.WalkieTalkieRoom
import com.meshverse.app.domain.repository.WalkieTalkieRepository
import com.meshverse.app.mesh.MeshEvent
import com.meshverse.app.mesh.MeshNetworkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WalkieTalkieManager
 *
 * Manages push-to-talk rooms with the following features:
 *  - Create / join / leave rooms (private, community, emergency, event, gaming)
 *  - Track who is currently speaking (currentSpeakerId)
 *  - Relay WALKIE_GROUP packets over the mesh for instant delivery
 *  - Emergency override channel always available
 *
 * Integration with VoiceCallService:
 *  - VoiceCallService sends VOICE_AUDIO packets tagged with the active roomId.
 *  - Incoming WALKIE_GROUP packets carry the roomId so receivers can filter.
 *
 * Walkie-talkie packet payload format (JSON):
 *  { "roomId": "...", "speakerId": "...", "action": "PTT_START"|"PTT_STOP" }
 */
@Singleton
class WalkieTalkieManager @Inject constructor(
    private val walkieTalkieRepository: WalkieTalkieRepository,
    private val meshNetworkManager: MeshNetworkManager,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "WalkieTalkieManager"
        private const val EMERGENCY_CHANNEL_NAME = "Emergency"
        private const val EMERGENCY_CHANNEL_NUMBER = 0
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeRoomId = MutableStateFlow<String?>(null)
    /** The room the local user is currently transmitting in. */
    val activeRoomId: StateFlow<String?> = _activeRoomId

    private val _incomingPttEvents = MutableStateFlow<PttEvent?>(null)
    /** Emit incoming PTT_START / PTT_STOP events for the UI layer. */
    val incomingPttEvents: StateFlow<PttEvent?> = _incomingPttEvents

    fun start() {
        scope.launch {
            ensureEmergencyChannel()
            meshNetworkManager.meshEvents.collectLatest { event ->
                if (event is MeshEvent.PacketReceived &&
                    event.packet.payloadType == PacketType.WALKIE_GROUP) {
                    handleIncomingGroupPacket(event.packet)
                }
            }
        }
        Log.d(TAG, "WalkieTalkieManager started")
    }

    fun stop() {
        scope.coroutineContext.cancelChildren()
    }

    // ── Room management ────────────────────────────────────────────────────

    suspend fun createRoom(
        name: String,
        channelType: ChannelType,
        channelNumber: Int,
        creatorId: String,
        encrypted: Boolean = true
    ): WalkieTalkieRoom {
        val room = WalkieTalkieRoom(
            name = name,
            channelType = channelType,
            channelNumber = channelNumber,
            creatorId = creatorId,
            memberIds = listOf(creatorId),
            isEncrypted = encrypted
        )
        walkieTalkieRepository.createRoom(room)
        Log.d(TAG, "Created walkie-talkie room: ${room.roomId} ($name)")
        return room
    }

    fun getAllRooms(): Flow<List<WalkieTalkieRoom>> = walkieTalkieRepository.getAllRooms()

    // ── PTT controls ───────────────────────────────────────────────────────

    /**
     * Start push-to-talk in the given room.
     * Broadcasts a PTT_START signal so all members see the "speaking" indicator.
     */
    fun startPtt(roomId: String, speakerId: String) {
        _activeRoomId.value = roomId
        scope.launch {
            walkieTalkieRepository.setActiveSpeaker(roomId, speakerId)
            broadcastPttSignal(roomId, speakerId, "PTT_START")
        }
    }

    /**
     * Stop push-to-talk and notify peers.
     */
    fun stopPtt(roomId: String, speakerId: String) {
        _activeRoomId.value = null
        scope.launch {
            walkieTalkieRepository.setActiveSpeaker(roomId, null)
            broadcastPttSignal(roomId, speakerId, "PTT_STOP")
        }
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private suspend fun broadcastPttSignal(roomId: String, speakerId: String, action: String) {
        val nodeId = meshNetworkManager.getLocalNodeId()
        val payload = gson.toJson(mapOf("roomId" to roomId, "speakerId" to speakerId, "action" to action))
        val packet = MeshPacket(
            sourceId = nodeId,
            destinationId = MeshNetworkManager.BROADCAST,
            senderId = nodeId,
            payloadType = PacketType.WALKIE_GROUP,
            payload = payload.toByteArray(Charsets.UTF_8),
            ttl = 4
        )
        meshNetworkManager.sendPacket(packet)
    }

    private fun handleIncomingGroupPacket(packet: MeshPacket) {
        try {
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson(String(packet.payload, Charsets.UTF_8), Map::class.java) as Map<String, String>
            val roomId    = map["roomId"]    ?: return
            val speakerId = map["speakerId"] ?: return
            val action    = map["action"]    ?: return

            val event = PttEvent(roomId = roomId, speakerId = speakerId, action = action)
            _incomingPttEvents.value = event

            scope.launch {
                when (action) {
                    "PTT_START" -> walkieTalkieRepository.setActiveSpeaker(roomId, speakerId)
                    "PTT_STOP"  -> walkieTalkieRepository.setActiveSpeaker(roomId, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling WALKIE_GROUP packet: ${e.message}")
        }
    }

    private suspend fun ensureEmergencyChannel() {
        if (walkieTalkieRepository.getEmergencyChannel() == null) {
            val emergency = WalkieTalkieRoom(
                name = EMERGENCY_CHANNEL_NAME,
                channelType = ChannelType.EMERGENCY,
                channelNumber = EMERGENCY_CHANNEL_NUMBER,
                creatorId = "system",
                isEncrypted = true
            )
            walkieTalkieRepository.createRoom(emergency)
            Log.d(TAG, "Emergency channel created")
        }
    }
}

data class PttEvent(
    val roomId: String,
    val speakerId: String,
    val action: String   // "PTT_START" | "PTT_STOP"
)
