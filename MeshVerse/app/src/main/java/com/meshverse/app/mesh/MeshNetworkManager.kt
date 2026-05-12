package com.meshverse.app.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.meshverse.app.domain.model.*
import com.meshverse.app.domain.repository.MeshRepository
import com.meshverse.app.domain.repository.MessageRepository
import com.meshverse.app.domain.repository.PeerRepository
import com.meshverse.app.security.CryptoManager
import com.meshverse.app.security.KeyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central mesh network manager implementing:
 * - AODV-like routing (Ad hoc On-Demand Distance Vector)
 * - Store-and-forward messaging
 * - Gossip protocol for content propagation
 * - Multi-hop relay with TTL
 * - Peer heartbeat and discovery
 */
@Singleton
class MeshNetworkManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectionsClient: ConnectionsClient,
    private val peerRepository: PeerRepository,
    private val messageRepository: MessageRepository,
    private val meshRepository: MeshRepository,
    private val keyManager: KeyManager,
    private val cryptoManager: CryptoManager,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "MeshNetworkManager"
        const val SERVICE_ID = "com.meshverse.app.mesh"
        private const val HEARTBEAT_INTERVAL_MS = 15_000L
        private const val ROUTE_TTL_MS = 300_000L  // 5 minutes
        private const val STALE_PEER_THRESHOLD_MS = 60_000L
        private const val MAX_TRANSMIT_RETRIES = 3
        private const val RETRY_BACKOFF_BASE_MS = 100L

        // Broadcast address
        const val BROADCAST = "BROADCAST"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track active Nearby connections: endpointId -> Peer
    private val activeConnections = ConcurrentHashMap<String, Peer>()

    // Seen packet IDs to prevent loops/duplicates.
    // LinkedHashMap maintains insertion order so removeEldestEntry evicts the oldest entry,
    // giving true LRU semantics. Access is guarded by the map's own monitor.
    private val seenPackets = Collections.synchronizedMap(
        object : LinkedHashMap<String, Unit>(16, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>?) =
                size > 10_000
        }
    )

    // Pending routed messages: destinationId -> list of packets waiting for route
    private val pendingPackets = ConcurrentHashMap<String, MutableList<MeshPacket>>()

    private val _connectedPeerCount = MutableStateFlow(0)
    val connectedPeerCount: StateFlow<Int> = _connectedPeerCount

    private val _meshEvents = MutableStateFlow<MeshEvent>(MeshEvent.Idle)
    val meshEvents: StateFlow<MeshEvent> = _meshEvents

    private var localNodeId: String = ""
    private var isRunning = false

    /** Returns true if the packet ID was new and was recorded; false if already seen. */
    private fun addIfNewPacket(packetId: String): Boolean =
        seenPackets.put(packetId, Unit) == null

    /** Exposes the current local node ID for services that embed it in packets. */
    fun getLocalNodeId(): String = localNodeId

    fun initialize(nodeId: String) {
        localNodeId = nodeId
        Log.d(TAG, "MeshNetworkManager initialized with nodeId=$nodeId")
    }

    /** Start advertising and discovering on Nearby Connections */
    fun startMesh() {
        if (isRunning) return
        isRunning = true

        startAdvertising()
        startDiscovery()
        startHeartbeat()
        startRouteMaintenanceJob()

        Log.d(TAG, "Mesh network started")
    }

    fun stopMesh() {
        isRunning = false
        scope.coroutineContext.cancelChildren()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        activeConnections.clear()
        _connectedPeerCount.value = 0
        Log.d(TAG, "Mesh network stopped")
    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startAdvertising(
            localNodeId,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Advertising failed: ${e.message}")
        }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Discovery failed: ${e.message}")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: $endpointId (${info.endpointName})")
            // Request connection to discovered peer
            connectionsClient.requestConnection(
                localNodeId,
                endpointId,
                connectionLifecycleCallback
            ).addOnFailureListener { e ->
                Log.w(TAG, "Connection request failed to $endpointId: ${e.message}")
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            handlePeerDisconnected(endpointId)
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated: $endpointId (${info.endpointName})")
            // Auto-accept all connections (in production, verify with Signal Protocol handshake)
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.d(TAG, "Connected to: $endpointId")
                handlePeerConnected(endpointId, result)
            } else {
                Log.w(TAG, "Connection failed to $endpointId: ${result.status.statusCode}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from: $endpointId")
            handlePeerDisconnected(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                scope.launch { processIncomingPacket(endpointId, bytes) }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Track transfer progress for large media chunks
            Log.v(TAG, "Transfer update from $endpointId: ${update.bytesTransferred}/${update.totalBytes}")
        }
    }

    private fun handlePeerConnected(endpointId: String, result: ConnectionResolution) {
        scope.launch {
            val peer = Peer(
                peerId = endpointId,
                deviceName = endpointId,
                userId = null,
                connectionType = ConnectionType.NEARBY,
                isConnected = true,
                routingCost = 1
            )
            activeConnections[endpointId] = peer
            peerRepository.upsertPeer(peer)
            _connectedPeerCount.value = activeConnections.size
            _meshEvents.value = MeshEvent.PeerConnected(peer)

            // Exchange key handshake
            sendKeyExchange(endpointId)

            // Deliver any pending messages for this peer
            deliverPendingMessages()
        }
    }

    private fun handlePeerDisconnected(endpointId: String) {
        scope.launch {
            activeConnections.remove(endpointId)
            peerRepository.updateConnection(endpointId, false)
            meshRepository.invalidateRoutesVia(endpointId)
            _connectedPeerCount.value = activeConnections.size
            _meshEvents.value = MeshEvent.PeerDisconnected(endpointId)
        }
    }

    /**
     * Send a MeshPacket to a destination.
     * If destination is directly connected, send immediately.
     * If not, look up best route and relay through next hop.
     * If no route, initiate route discovery (RREQ) and queue.
     */
    suspend fun sendPacket(packet: MeshPacket) {
        if (packet.destinationId == BROADCAST) {
            broadcastPacket(packet)
            return
        }

        // Check direct connection
        if (activeConnections.containsKey(packet.destinationId)) {
            transmitToEndpoint(packet.destinationId, packet)
            return
        }

        // Look up routing table
        val route = meshRepository.getBestRoute(packet.destinationId)
        if (route != null && route.isValid) {
            transmitToEndpoint(route.nextHopId, packet)
            return
        }

        // No route: send RREQ and queue packet
        Log.d(TAG, "No route to ${packet.destinationId}, initiating route discovery")
        pendingPackets.getOrPut(packet.destinationId) { mutableListOf() }.add(packet)
        sendRouteRequest(packet.destinationId)
    }

    /** Broadcast packet to all connected peers (gossip) */
    private fun broadcastPacket(packet: MeshPacket) {
        if (!addIfNewPacket(packet.packetId)) return

        activeConnections.keys.forEach { endpointId ->
            scope.launch { transmitToEndpoint(endpointId, packet) }
        }
    }

    private suspend fun transmitToEndpoint(endpointId: String, packet: MeshPacket) {
        var lastError: Exception? = null
        repeat(MAX_TRANSMIT_RETRIES) { attempt ->
            try {
                val json = gson.toJson(packet)
                val payload = Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
                connectionsClient.sendPayload(endpointId, payload)
                Log.v(TAG, "Sent packet ${packet.packetId} to $endpointId")
                return
            } catch (e: Exception) {
                lastError = e
                if (attempt < MAX_TRANSMIT_RETRIES - 1) {
                    delay(calculateRetryBackoff(attempt))
                }
            }
        }
        Log.e(TAG, "Failed to transmit to $endpointId after $MAX_TRANSMIT_RETRIES attempts: ${lastError?.message}")
        if (packet.destinationId != BROADCAST) {
            // Broadcast delivery is opportunistic by design; route errors are only meaningful for unicast paths.
            // endpointId is the failed next-hop endpoint; route invalidation should target that hop.
            sendRouteError(endpointId)
        }
    }

    private fun calculateRetryBackoff(attempt: Int): Long {
        val retryMultiplier = attempt + 1
        return RETRY_BACKOFF_BASE_MS * retryMultiplier * retryMultiplier
    }

    /**
     * Process received packet:
     * 1. Deduplicate
     * 2. Check if destination is local
     * 3. If not, decrement TTL and relay
     */
    private suspend fun processIncomingPacket(fromEndpointId: String, bytes: ByteArray) {
        try {
            val json = String(bytes, Charsets.UTF_8)
            val packet = gson.fromJson(json, MeshPacket::class.java)

            // Deduplication: atomically record the packet; drop if already seen
            if (!addIfNewPacket(packet.packetId)) {
                Log.v(TAG, "Duplicate packet ${packet.packetId}, dropping")
                return
            }

            Log.d(TAG, "Received packet type=${packet.payloadType} from=$fromEndpointId dest=${packet.destinationId}")

            when (packet.payloadType) {
                PacketType.ROUTE_REQUEST -> handleRouteRequest(packet, fromEndpointId)
                PacketType.ROUTE_REPLY -> handleRouteReply(packet, fromEndpointId)
                PacketType.ROUTE_ERROR -> handleRouteError(packet)
                PacketType.HEARTBEAT -> handleHeartbeat(packet, fromEndpointId)
                PacketType.KEY_EXCHANGE -> handleKeyExchange(packet, fromEndpointId)
                PacketType.ACK -> handleAck(packet)
                else -> {
                    if (packet.destinationId == localNodeId || packet.destinationId == BROADCAST) {
                        // Deliver to local handlers
                        _meshEvents.value = MeshEvent.PacketReceived(packet)
                        if (packet.destinationId == BROADCAST && packet.ttl > 0) {
                            relayBroadcastPacket(packet, fromEndpointId)
                        }
                    } else if (packet.ttl > 0) {
                        // Relay: decrement TTL and forward
                        val relayPacket = packet.copy(
                            ttl = packet.ttl - 1,
                            hopCount = packet.hopCount + 1,
                            senderId = localNodeId
                        )
                        sendPacket(relayPacket)
                    } else {
                        Log.d(TAG, "TTL expired for packet ${packet.packetId}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing packet from $fromEndpointId: ${e.message}")
        }
    }

    /**
     * Re-broadcast packet to neighbours (except sender) for multi-hop DTN propagation.
     * Keeps packetId unchanged for network-wide deduplication.
     */
    private fun relayBroadcastPacket(packet: MeshPacket, fromEndpointId: String) {
        val relayPacket = packet.copy(
            ttl = packet.ttl - 1,
            hopCount = packet.hopCount + 1,
            senderId = localNodeId
        )
        activeConnections.keys
            .asSequence()
            .filter { it != fromEndpointId }
            .forEach { endpointId ->
                scope.launch { transmitToEndpoint(endpointId, relayPacket) }
            }
    }

    // ===== AODV Routing =====

    private suspend fun sendRouteRequest(destinationId: String) {
        val rreq = MeshPacket(
            sourceId = localNodeId,
            destinationId = BROADCAST,
            senderId = localNodeId,
            payloadType = PacketType.ROUTE_REQUEST,
            payload = destinationId.toByteArray()
        )
        broadcastPacket(rreq)
    }

    private suspend fun handleRouteRequest(packet: MeshPacket, fromEndpointId: String) {
        val targetId = String(packet.payload)

        // Update reverse route to packet source
        val reverseRoute = MeshRoute(
            routeId = UUID.randomUUID().toString(),
            destinationId = packet.sourceId,
            nextHopId = fromEndpointId,
            hopCount = packet.hopCount + 1,
            routingCost = packet.hopCount + 1,
            expiresAt = System.currentTimeMillis() + ROUTE_TTL_MS
        )
        meshRepository.addRoute(reverseRoute)

        if (targetId == localNodeId) {
            // We are the destination: send RREP back
            sendRouteReply(packet.sourceId, fromEndpointId, packet.hopCount + 1)
        } else if (packet.ttl > 0) {
            // Forward RREQ
            val forwarded = packet.copy(
                ttl = packet.ttl - 1,
                hopCount = packet.hopCount + 1,
                senderId = localNodeId
            )
            broadcastPacket(forwarded)
        }
    }

    private suspend fun sendRouteReply(destinationId: String, nextHopId: String, hopCount: Int) {
        val rrep = MeshPacket(
            sourceId = localNodeId,
            destinationId = destinationId,
            senderId = localNodeId,
            payloadType = PacketType.ROUTE_REPLY,
            payload = "$localNodeId:$hopCount".toByteArray()
        )
        transmitToEndpoint(nextHopId, rrep)
    }

    private suspend fun handleRouteReply(packet: MeshPacket, fromEndpointId: String) {
        val parts = String(packet.payload).split(":")
        val originId = parts[0]
        val hopCount = parts.getOrNull(1)?.toIntOrNull() ?: 1

        // Install forward route
        val route = MeshRoute(
            routeId = UUID.randomUUID().toString(),
            destinationId = originId,
            nextHopId = fromEndpointId,
            hopCount = hopCount,
            routingCost = hopCount,
            expiresAt = System.currentTimeMillis() + ROUTE_TTL_MS
        )
        meshRepository.addRoute(route)

        // Deliver pending packets to this destination
        pendingPackets.remove(originId)?.forEach { pendingPacket ->
            sendPacket(pendingPacket)
        }

        // If RREP not for us, forward it
        if (packet.destinationId != localNodeId) {
            val forwardRoute = meshRepository.getBestRoute(packet.destinationId)
            forwardRoute?.let { transmitToEndpoint(it.nextHopId, packet) }
        }
    }

    private fun handleRouteError(packet: MeshPacket) {
        val brokenNodeId = String(packet.payload)
        scope.launch {
            meshRepository.invalidateRoutesVia(brokenNodeId)
        }
    }

    // ===== Heartbeat =====

    private fun startHeartbeat() {
        scope.launch {
            while (isRunning) {
                delay(HEARTBEAT_INTERVAL_MS)
                sendHeartbeat()
                peerRepository.markStaleAsDisconnected(STALE_PEER_THRESHOLD_MS)
            }
        }
    }

    private fun sendHeartbeat() {
        scope.launch {
            val heartbeat = MeshPacket(
                sourceId = localNodeId,
                destinationId = BROADCAST,
                senderId = localNodeId,
                payloadType = PacketType.HEARTBEAT,
                payload = keyManager.getPublicKeyBase64().toByteArray()
            )
            broadcastPacket(heartbeat)
        }
    }

    private fun handleHeartbeat(packet: MeshPacket, fromEndpointId: String) {
        scope.launch {
            peerRepository.updateConnection(fromEndpointId, true)
        }
    }

    // ===== Key Exchange =====

    private fun sendKeyExchange(endpointId: String) {
        scope.launch {
            val keyPacket = MeshPacket(
                sourceId = localNodeId,
                destinationId = endpointId,
                senderId = localNodeId,
                payloadType = PacketType.KEY_EXCHANGE,
                payload = keyManager.getPublicKeyBase64().toByteArray()
            )
            transmitToEndpoint(endpointId, keyPacket)
        }
    }

    private fun handleKeyExchange(packet: MeshPacket, fromEndpointId: String) {
        val peerPublicKey = String(packet.payload)
        scope.launch {
            val existing = peerRepository.getConnectedPeersOnce()
                .find { it.peerId == fromEndpointId }
            existing?.let {
                peerRepository.upsertPeer(it.copy(publicKey = peerPublicKey))
            }
        }
    }

    private fun handleAck(packet: MeshPacket) {
        val messageId = String(packet.payload)
        scope.launch {
            messageRepository.markDelivered(messageId)
        }
    }

    private fun sendRouteError(brokenNodeId: String) {
        scope.launch {
            val rerr = MeshPacket(
                sourceId = localNodeId,
                destinationId = BROADCAST,
                senderId = localNodeId,
                payloadType = PacketType.ROUTE_ERROR,
                payload = brokenNodeId.toByteArray()
            )
            broadcastPacket(rerr)
        }
    }

    private fun startRouteMaintenanceJob() {
        scope.launch {
            while (isRunning) {
                delay(60_000L)
                meshRepository.pruneStaleRoutes()
            }
        }
    }

    private fun deliverPendingMessages() {
        scope.launch {
            val pending = messageRepository.getPendingMessages()
            pending.forEach { msg ->
                val packet = MeshPacket(
                    sourceId = localNodeId,
                    destinationId = msg.recipientId,
                    senderId = localNodeId,
                    payloadType = PacketType.MESSAGE,
                    payload = gson.toJson(msg).toByteArray()
                )
                sendPacket(packet)
            }
        }
    }

    /** Send SOS emergency broadcast */
    suspend fun sendSOS(userId: String, lat: Double, lon: Double, message: String) {
        val sosData = """{"userId":"$userId","lat":$lat,"lon":$lon,"msg":"$message","ts":${System.currentTimeMillis()}}"""
        val sosPacket = MeshPacket(
            sourceId = localNodeId,
            destinationId = BROADCAST,
            senderId = localNodeId,
            payloadType = PacketType.SOS_BROADCAST,
            payload = sosData.toByteArray(),
            ttl = 10
        )
        broadcastPacket(sosPacket)
        Log.w(TAG, "SOS broadcast sent from $userId at ($lat,$lon)")
    }
}

sealed class MeshEvent {
    object Idle : MeshEvent()
    data class PeerConnected(val peer: Peer) : MeshEvent()
    data class PeerDisconnected(val peerId: String) : MeshEvent()
    data class PacketReceived(val packet: MeshPacket) : MeshEvent()
    data class SosReceived(val userId: String, val lat: Double, val lon: Double, val message: String) : MeshEvent()
}
