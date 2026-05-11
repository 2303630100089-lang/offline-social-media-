package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.WalkieTalkieRoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkieTalkieRoomDao {

    @Upsert
    suspend fun upsertRoom(room: WalkieTalkieRoomEntity)

    @Query("SELECT * FROM walkie_talkie_rooms ORDER BY lastActivityAt DESC")
    fun getAllRooms(): Flow<List<WalkieTalkieRoomEntity>>

    @Query("SELECT * FROM walkie_talkie_rooms WHERE roomId = :roomId LIMIT 1")
    suspend fun getRoomById(roomId: String): WalkieTalkieRoomEntity?

    @Query("SELECT * FROM walkie_talkie_rooms WHERE channelType = 'EMERGENCY' ORDER BY createdAt ASC LIMIT 1")
    suspend fun getEmergencyChannel(): WalkieTalkieRoomEntity?

    @Query("UPDATE walkie_talkie_rooms SET isActive = :active, currentSpeakerId = :speakerId, lastActivityAt = :now WHERE roomId = :roomId")
    suspend fun updateActivity(roomId: String, active: Boolean, speakerId: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE walkie_talkie_rooms SET currentSpeakerId = NULL, isActive = 0 WHERE roomId = :roomId")
    suspend fun clearSpeaker(roomId: String)

    @Query("DELETE FROM walkie_talkie_rooms WHERE roomId = :roomId")
    suspend fun deleteRoom(roomId: String)
}
