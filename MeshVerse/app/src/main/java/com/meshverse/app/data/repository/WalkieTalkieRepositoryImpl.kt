package com.meshverse.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meshverse.app.data.local.dao.WalkieTalkieRoomDao
import com.meshverse.app.data.local.entity.WalkieTalkieRoomEntity
import com.meshverse.app.domain.model.ChannelType
import com.meshverse.app.domain.model.WalkieTalkieRoom
import com.meshverse.app.domain.repository.WalkieTalkieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WalkieTalkieRepositoryImpl @Inject constructor(
    private val dao: WalkieTalkieRoomDao,
    private val gson: Gson
) : WalkieTalkieRepository {

    override fun getAllRooms(): Flow<List<WalkieTalkieRoom>> =
        dao.getAllRooms().map { it.map { e -> e.toDomain() } }

    override suspend fun getRoomById(roomId: String): WalkieTalkieRoom? =
        dao.getRoomById(roomId)?.toDomain()

    override suspend fun getEmergencyChannel(): WalkieTalkieRoom? =
        dao.getEmergencyChannel()?.toDomain()

    override suspend fun createRoom(room: WalkieTalkieRoom) =
        dao.upsertRoom(room.toEntity())

    override suspend fun setActiveSpeaker(roomId: String, speakerId: String?) {
        if (speakerId != null) {
            dao.updateActivity(roomId, active = true, speakerId = speakerId)
        } else {
            dao.clearSpeaker(roomId)
        }
    }

    override suspend fun deleteRoom(roomId: String) = dao.deleteRoom(roomId)

    // ── Mapping ────────────────────────────────────────────────────────────

    private fun WalkieTalkieRoomEntity.toDomain(): WalkieTalkieRoom {
        val memberType = object : TypeToken<List<String>>() {}.type
        val members: List<String> = runCatching {
            gson.fromJson<List<String>>(memberIdsJson, memberType).orEmpty()
        }.getOrDefault(emptyList())
        return WalkieTalkieRoom(
            roomId = roomId,
            name = name,
            description = description,
            channelType = runCatching { ChannelType.valueOf(channelType) }.getOrDefault(ChannelType.PRIVATE),
            channelNumber = channelNumber,
            creatorId = creatorId,
            memberIds = members,
            isEncrypted = isEncrypted,
            isActive = isActive,
            currentSpeakerId = currentSpeakerId,
            createdAt = createdAt,
            lastActivityAt = lastActivityAt,
            meshAddress = meshAddress
        )
    }

    private fun WalkieTalkieRoom.toEntity() = WalkieTalkieRoomEntity(
        roomId = roomId,
        name = name,
        description = description,
        channelType = channelType.name,
        channelNumber = channelNumber,
        creatorId = creatorId,
        memberIdsJson = gson.toJson(memberIds),
        isEncrypted = isEncrypted,
        isActive = isActive,
        currentSpeakerId = currentSpeakerId,
        createdAt = createdAt,
        lastActivityAt = lastActivityAt,
        meshAddress = meshAddress
    )
}
