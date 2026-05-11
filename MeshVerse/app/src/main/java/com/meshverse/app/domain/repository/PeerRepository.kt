package com.meshverse.app.domain.repository

import com.meshverse.app.domain.model.Peer
import kotlinx.coroutines.flow.Flow

interface PeerRepository {
    fun getConnectedPeers(): Flow<List<Peer>>
    suspend fun getConnectedPeersOnce(): List<Peer>
    fun getAllPeers(): Flow<List<Peer>>
    suspend fun upsertPeer(peer: Peer)
    suspend fun updateConnection(peerId: String, connected: Boolean)
    fun getConnectedPeerCount(): Flow<Int>
    suspend fun markStaleAsDisconnected(thresholdMs: Long)
}
