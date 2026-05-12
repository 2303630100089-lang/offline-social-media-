package com.meshverse.app.sync

import android.util.Log
import android.util.Base64
import com.google.gson.Gson
import com.meshverse.app.domain.model.MapReport
import com.meshverse.app.domain.model.MeshPacket
import com.meshverse.app.domain.model.PacketType
import com.meshverse.app.domain.model.Peer
import com.meshverse.app.domain.model.Poll
import com.meshverse.app.domain.repository.MapReportRepository
import com.meshverse.app.domain.repository.MessageRepository
import com.meshverse.app.domain.repository.PeerRepository
import com.meshverse.app.domain.repository.PollRepository
import com.meshverse.app.domain.repository.PostRepository
import com.meshverse.app.mesh.MeshEvent
import com.meshverse.app.mesh.MeshNetworkManager
import com.meshverse.app.security.CryptoManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gossip Synchronization Manager
 *
 * Implements a CRDT-inspired peer gossip protocol for decentralised data propagation.
 *
 * Algorithm overview:
 *  1. On peer connect  → exchange sync digests (GOSSIP_HELLO).
 *  2. On digest receipt → identify missing/newer items and send deltas (GOSSIP_DELTA).
 *  3. On delta receipt  → apply using LWW / version-vector merge, then re-propagate once.
 *
 * This achieves eventual consistency without a central server:
 *  - If User A posts offline and meets User B, the post syncs.
 *  - When User B later meets User C, the post propagates further.
 *
 * Delta packet format (JSON):
 *  { "type": "POLL"|"POST"|"MAP_REPORT", "payload": <serialised object> }
 *
 * Duplicate prevention:
 *  - Each item carries a `version` (Lamport clock).
 *  - An item is only forwarded if its version is newer than what we have locally.
 */
@Singleton
class GossipSyncManager @Inject constructor(
    private val meshNetworkManager: MeshNetworkManager,
    private val postRepository: PostRepository,
    private val pollRepository: PollRepository,
    private val mapReportRepository: MapReportRepository,
    private val messageRepository: MessageRepository,
    private val peerRepository: PeerRepository,
    private val cryptoManager: CryptoManager,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "GossipSyncManager"
        private const val GOSSIP_PUSH_INTERVAL_MS = 30_000L
        private const val COMPRESSION_THRESHOLD_BYTES = 512
        private const val STREAM_BUFFER_BYTES = 1024
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false

    /** Start listening for mesh events and schedule periodic push. */
    fun start() {
        if (isRunning) return
        isRunning = true

        // React to peer-connect events by pushing un-synced deltas
        scope.launch {
            meshNetworkManager.meshEvents.collectLatest { event ->
                when (event) {
                    is MeshEvent.PeerConnected -> pushDeltasToPeer(event.peer.peerId)
                    is MeshEvent.PacketReceived -> handleGossipPacket(event.packet)
                    else -> Unit
                }
            }
        }

        // Periodic push to all connected peers
        scope.launch {
            while (isRunning) {
                delay(GOSSIP_PUSH_INTERVAL_MS)
                broadcastPendingDeltas()
            }
        }

        Log.d(TAG, "GossipSyncManager started")
    }

    fun stop() {
        isRunning = false
        scope.coroutineContext.cancelChildren()
    }

    // ── Outbound ───────────────────────────────────────────────────────────

    private suspend fun pushDeltasToPeer(peerId: String) {
        Log.d(TAG, "Pushing deltas to newly connected peer: $peerId")
        val peer = peerRepository.getConnectedPeersOnce().find { it.peerId == peerId } ?: return
        pushPendingDeltasToPeers(listOf(peer))
    }

    private suspend fun broadcastPendingDeltas() {
        val nodeId = meshNetworkManager.getLocalNodeId()
        val peers = peerRepository.getConnectedPeersOnce()
        if (peers.isEmpty()) return
        pushPendingDeltasToPeers(peers, nodeId)
    }

    private suspend fun pushPendingDeltasToPeers(
        peers: List<Peer>,
        nodeId: String = meshNetworkManager.getLocalNodeId()
    ) {
        if (peers.isEmpty()) return

        // Push unsynced polls
        pollRepository.getUnSyncedPolls().forEach { poll ->
            val delta = GossipDelta("POLL", gson.toJson(poll))
            peers.forEach { peer ->
                sendDelta(nodeId, peer, delta)
            }
            pollRepository.markSynced(poll.pollId)
        }

        // Push unsynced map reports
        mapReportRepository.getUnSyncedReports().forEach { report ->
            val delta = GossipDelta("MAP_REPORT", gson.toJson(report))
            peers.forEach { peer ->
                sendDelta(nodeId, peer, delta)
            }
            mapReportRepository.markSynced(report.reportId)
        }
    }

    private suspend fun sendDelta(nodeId: String, peer: Peer, delta: GossipDelta) {
        val plainDeltaJson = gson.toJson(delta)
        val compressed = plainDeltaJson.toByteArray(Charsets.UTF_8).size >= COMPRESSION_THRESHOLD_BYTES
        val compressedBody = if (compressed) {
            compressToBase64(plainDeltaJson.toByteArray(Charsets.UTF_8))
        } else {
            plainDeltaJson
        }
        val peerKey = peer.publicKey?.takeIf { it.isNotBlank() }
        val encrypted = peerKey != null
        val body = peerKey?.let { cryptoManager.encrypt(compressedBody, it) } ?: compressedBody
        val envelope = GossipTransportEnvelope(
            encrypted = encrypted,
            compressed = compressed,
            body = body
        )
        val packet = MeshPacket(
            sourceId = nodeId,
            destinationId = peer.peerId,
            senderId = nodeId,
            payloadType = PacketType.GOSSIP_DELTA,
            payload = gson.toJson(envelope).toByteArray(Charsets.UTF_8),
            ttl = 5  // Limited propagation range for gossip
        )
        meshNetworkManager.sendPacket(packet)
    }

    // ── Inbound ────────────────────────────────────────────────────────────

    private suspend fun handleGossipPacket(packet: MeshPacket) {
        if (packet.payloadType != PacketType.GOSSIP_DELTA) return

        try {
            val json = String(packet.payload, Charsets.UTF_8)
            val delta = decodeDelta(packet, json) ?: return
            Log.d(TAG, "Received gossip delta type=${delta.type}")

            when (delta.type) {
                "POLL" -> {
                    val poll = gson.fromJson(delta.payload, Poll::class.java)
                    pollRepository.mergePollFromPeer(poll)
                }
                "MAP_REPORT" -> {
                    val report = gson.fromJson(delta.payload, MapReport::class.java)
                    // Increment hop count before storing, then re-propagate
                    mapReportRepository.addReport(report.copy(propagationHops = report.propagationHops + 1))
                }
                else -> Log.w(TAG, "Unknown gossip delta type: ${delta.type}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing gossip packet: ${e.message}")
        }
    }

    private suspend fun decodeDelta(packet: MeshPacket, payloadJson: String): GossipDelta? {
        val envelope = runCatching {
            gson.fromJson(payloadJson, GossipTransportEnvelope::class.java)
        }.getOrNull()

        if (envelope == null || envelope.body.isBlank()) {
            return runCatching { gson.fromJson(payloadJson, GossipDelta::class.java) }.getOrNull()
        }

        var decodedBody = envelope.body
        if (envelope.encrypted) {
            val senderPublicKey = peerRepository.getConnectedPeersOnce()
                .firstOrNull { it.peerId == packet.sourceId || it.peerId == packet.senderId }
                ?.publicKey
                ?: run {
                    Log.w(TAG, "Skipping encrypted gossip delta: missing sender key for source=${packet.sourceId}")
                    return null
                }
            decodedBody = cryptoManager.decrypt(envelope.body, senderPublicKey)
        }

        if (envelope.compressed) {
            decodedBody = inflateFromBase64(decodedBody).toString(Charsets.UTF_8)
        }

        return gson.fromJson(decodedBody, GossipDelta::class.java)
    }

    private fun compressToBase64(data: ByteArray): String {
        val deflater = Deflater(Deflater.BEST_SPEED)
        deflater.setInput(data)
        deflater.finish()
        val buffer = ByteArray(STREAM_BUFFER_BYTES)
        val output = ByteArrayOutputStream()
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            output.write(buffer, 0, count)
        }
        deflater.end()
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun inflateFromBase64(base64: String): ByteArray {
        return try {
            val compressed = Base64.decode(base64, Base64.NO_WRAP)
            val inflater = Inflater()
            inflater.setInput(compressed)
            val buffer = ByteArray(STREAM_BUFFER_BYTES)
            val output = ByteArrayOutputStream()
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count <= 0) break
                output.write(buffer, 0, count)
            }
            inflater.end()
            output.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to inflate gossip payload, using decoded bytes fallback: ${e.message}")
            Base64.decode(base64, Base64.NO_WRAP)
        }
    }
}

/** Wire format for a single gossip delta. */
data class GossipDelta(
    val type: String,       // "POLL", "MAP_REPORT", "POST"
    val payload: String     // JSON-serialised object
)

data class GossipTransportEnvelope(
    val encrypted: Boolean,
    val compressed: Boolean,
    val body: String
)
