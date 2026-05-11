package com.meshverse.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meshverse.app.data.local.dao.PeerDao
import com.meshverse.app.data.local.entity.PeerEntity
import com.meshverse.app.domain.model.ConnectionType
import com.meshverse.app.domain.model.Peer
import com.meshverse.app.domain.model.PeerCapability
import com.meshverse.app.domain.repository.PeerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PeerRepositoryImpl @Inject constructor(
    private val peerDao: PeerDao,
    private val gson: Gson
) : PeerRepository {

    override fun getConnectedPeers(): Flow<List<Peer>> =
        peerDao.getConnectedPeers().map { it.map { e -> e.toDomain() } }

    override suspend fun getConnectedPeersOnce(): List<Peer> =
        peerDao.getConnectedPeersOnce().map { it.toDomain() }

    override fun getAllPeers(): Flow<List<Peer>> =
        peerDao.getAllPeers().map { it.map { e -> e.toDomain() } }

    override suspend fun upsertPeer(peer: Peer) =
        peerDao.insert(peer.toEntity())

    override suspend fun updateConnection(peerId: String, connected: Boolean) =
        peerDao.updateConnection(peerId, connected)

    override fun getConnectedPeerCount(): Flow<Int> =
        peerDao.getConnectedPeerCount()

    override suspend fun markStaleAsDisconnected(thresholdMs: Long) =
        peerDao.markStaleAsDisconnected(System.currentTimeMillis() - thresholdMs)

    private fun PeerEntity.toDomain(): Peer {
        val caps: List<String> = capabilities?.let {
            gson.fromJson(it, object : TypeToken<List<String>>() {}.type)
        } ?: emptyList()
        return Peer(
            peerId = peerId,
            deviceName = deviceName,
            userId = userId,
            connectionType = runCatching { ConnectionType.valueOf(connectionType.uppercase()) }
                .getOrDefault(ConnectionType.NEARBY),
            signalStrength = signalStrength,
            latitude = latitude,
            longitude = longitude,
            distance = distance,
            isConnected = isConnected,
            isRelayNode = isRelayNode,
            routingCost = routingCost,
            lastSeen = lastSeen,
            publicKey = publicKey,
            capabilities = caps.mapNotNull { runCatching { PeerCapability.valueOf(it.uppercase()) }.getOrNull() },
            batteryLevel = batteryLevel,
            reputation = reputation
        )
    }

    private fun Peer.toEntity() = PeerEntity(
        peerId = peerId,
        deviceName = deviceName,
        userId = userId,
        connectionType = connectionType.name.lowercase(),
        signalStrength = signalStrength,
        latitude = latitude,
        longitude = longitude,
        distance = distance,
        isConnected = isConnected,
        isRelayNode = isRelayNode,
        routingCost = routingCost,
        lastSeen = lastSeen,
        publicKey = publicKey,
        capabilities = if (capabilities.isEmpty()) null else gson.toJson(capabilities.map { it.name.lowercase() }),
        batteryLevel = batteryLevel,
        reputation = reputation
    )
}
