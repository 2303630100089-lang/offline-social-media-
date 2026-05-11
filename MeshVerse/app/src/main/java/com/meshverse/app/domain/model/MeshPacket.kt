package com.meshverse.app.domain.model

import java.util.UUID

/**
 * MeshPacket is the fundamental unit of data transmitted over the mesh network.
 * Supports store-and-forward, multi-hop relay, and deduplication.
 */
data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val sourceId: String,           // Originating node
    val destinationId: String,      // Target node ("BROADCAST" for broadcast)
    val senderId: String,           // Current hop sender
    val payloadType: PacketType,
    val payload: ByteArray,         // Encrypted payload bytes
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 7,               // Time-to-live (max hops)
    val hopCount: Int = 0,
    val sequenceNumber: Int = 0,
    val signature: ByteArray? = null // Ed25519 signature of (packetId+payload)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeshPacket) return false
        return packetId == other.packetId
    }
    override fun hashCode(): Int = packetId.hashCode()
}

enum class PacketType {
    MESSAGE,           // Chat message
    ROUTE_REQUEST,     // AODV RREQ
    ROUTE_REPLY,       // AODV RREP
    ROUTE_ERROR,       // AODV RERR
    HEARTBEAT,         // Peer alive signal
    SOS_BROADCAST,     // Emergency SOS
    POST_SYNC,         // Social post propagation
    KEY_EXCHANGE,      // Diffie-Hellman key exchange
    ACK,               // Delivery acknowledgement
    MEDIA_CHUNK,       // Chunked media transfer
    GOSSIP             // Gossip protocol sync
}
