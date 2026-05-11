package com.meshverse.app.domain.repository

import com.meshverse.app.domain.model.WalkieTalkieRoom
import kotlinx.coroutines.flow.Flow

interface WalkieTalkieRepository {
    fun getAllRooms(): Flow<List<WalkieTalkieRoom>>
    suspend fun getRoomById(roomId: String): WalkieTalkieRoom?
    suspend fun getEmergencyChannel(): WalkieTalkieRoom?
    suspend fun createRoom(room: WalkieTalkieRoom)
    suspend fun setActiveSpeaker(roomId: String, speakerId: String?)
    suspend fun deleteRoom(roomId: String)
}
