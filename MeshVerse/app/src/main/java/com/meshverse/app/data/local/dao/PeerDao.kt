package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.PeerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(peer: PeerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(peers: List<PeerEntity>)

    @Update
    suspend fun update(peer: PeerEntity)

    @Query("SELECT * FROM peers WHERE isConnected = 1 ORDER BY routingCost ASC")
    fun getConnectedPeers(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers WHERE isConnected = 1 ORDER BY routingCost ASC")
    suspend fun getConnectedPeersOnce(): List<PeerEntity>

    @Query("SELECT * FROM peers ORDER BY lastSeen DESC")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers WHERE peerId = :peerId")
    suspend fun getById(peerId: String): PeerEntity?

    @Query("UPDATE peers SET isConnected = :connected, lastSeen = :ts WHERE peerId = :peerId")
    suspend fun updateConnection(peerId: String, connected: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE peers SET isConnected = 0 WHERE lastSeen < :threshold")
    suspend fun markStaleAsDisconnected(threshold: Long)

    @Query("SELECT COUNT(*) FROM peers WHERE isConnected = 1")
    fun getConnectedPeerCount(): Flow<Int>
}
