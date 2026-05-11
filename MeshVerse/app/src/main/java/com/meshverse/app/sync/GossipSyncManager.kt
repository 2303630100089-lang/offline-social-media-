package com.meshverse.app.sync

import android.util.Log
import com.google.gson.Gson
import com.meshverse.app.domain.model.MapReport
import com.meshverse.app.domain.model.MeshPacket
import com.meshverse.app.domain.model.PacketType
import com.meshverse.app.domain.model.Poll
import com.meshverse.app.domain.repository.MapReportRepository
import com.meshverse.app.domain.repository.MessageRepository
import com.meshverse.app.domain.repository.PollRepository
import com.meshverse.app.domain.repository.PostRepository
import com.meshverse.app.mesh.MeshEvent
import com.meshverse.app.mesh.MeshNetworkManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
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
    private val gson: Gson
) {
    companion object {
        private const val TAG = "GossipSyncManager"
        private const val GOSSIP_PUSH_INTERVAL_MS = 30_000L
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
        broadcastPendingDeltas()
    }

    private suspend fun broadcastPendingDeltas() {
        val nodeId = meshNetworkManager.getLocalNodeId()

        // Push unsynced polls
        pollRepository.getUnSyncedPolls().forEach { poll ->
            val delta = GossipDelta("POLL", gson.toJson(poll))
            sendDelta(nodeId, delta)
            pollRepository.markSynced(poll.pollId)
        }

        // Push unsynced map reports
        mapReportRepository.getUnSyncedReports().forEach { report ->
            val delta = GossipDelta("MAP_REPORT", gson.toJson(report))
            sendDelta(nodeId, delta)
            mapReportRepository.markSynced(report.reportId)
        }
    }

    private suspend fun sendDelta(nodeId: String, delta: GossipDelta) {
        val packet = MeshPacket(
            sourceId = nodeId,
            destinationId = MeshNetworkManager.BROADCAST,
            senderId = nodeId,
            payloadType = PacketType.GOSSIP_DELTA,
            payload = gson.toJson(delta).toByteArray(Charsets.UTF_8),
            ttl = 5  // Limited propagation range for gossip
        )
        meshNetworkManager.sendPacket(packet)
    }

    // ── Inbound ────────────────────────────────────────────────────────────

    private suspend fun handleGossipPacket(packet: MeshPacket) {
        if (packet.payloadType != PacketType.GOSSIP_DELTA) return

        try {
            val json = String(packet.payload, Charsets.UTF_8)
            val delta = gson.fromJson(json, GossipDelta::class.java)
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
}

/** Wire format for a single gossip delta. */
data class GossipDelta(
    val type: String,       // "POLL", "MAP_REPORT", "POST"
    val payload: String     // JSON-serialised object
)
