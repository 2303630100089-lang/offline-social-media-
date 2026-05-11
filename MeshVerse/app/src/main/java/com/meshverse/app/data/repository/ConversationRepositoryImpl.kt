package com.meshverse.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meshverse.app.data.local.dao.ConversationDao
import com.meshverse.app.data.local.entity.ConversationEntity
import com.meshverse.app.domain.model.Conversation
import com.meshverse.app.domain.model.ConversationType
import com.meshverse.app.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val gson: Gson
) : ConversationRepository {

    override fun getAllConversations(): Flow<List<Conversation>> =
        conversationDao.getAllActive().map { list -> list.map { it.toDomain() } }

    override suspend fun getConversationById(id: String): Conversation? =
        conversationDao.getById(id)?.toDomain()

    override suspend fun createConversation(conversation: Conversation) =
        conversationDao.insert(conversation.toEntity())

    override suspend fun updateLastMessage(conversationId: String, preview: String, timestamp: Long) {
        // We pass an empty msgId here; the actual message ID can be set separately if needed
        conversationDao.updateLastMessage(conversationId, "", preview, timestamp)
    }

    override suspend fun markRead(conversationId: String) =
        conversationDao.clearUnread(conversationId)

    private fun ConversationEntity.toDomain(): Conversation {
        val participants: List<String> = gson.fromJson(
            participantIds, object : TypeToken<List<String>>() {}.type
        ) ?: emptyList()
        val admins: List<String> = adminIds?.let {
            gson.fromJson(it, object : TypeToken<List<String>>() {}.type)
        } ?: emptyList()
        return Conversation(
            conversationId = conversationId,
            type = runCatching { ConversationType.valueOf(type.uppercase()) }.getOrDefault(ConversationType.DIRECT),
            name = name,
            description = description,
            avatarPath = avatarPath,
            participantIds = participants,
            adminIds = admins,
            lastMessagePreview = lastMessagePreview,
            lastMessageAt = lastMessageAt,
            unreadCount = unreadCount,
            isPinned = isPinned,
            isMuted = isMuted,
            encryptionEnabled = encryptionEnabled,
            locality = locality
        )
    }

    private fun Conversation.toEntity() = ConversationEntity(
        conversationId = conversationId,
        type = type.name.lowercase(),
        name = name,
        description = description,
        avatarPath = avatarPath,
        participantIds = gson.toJson(participantIds),
        adminIds = if (adminIds.isEmpty()) null else gson.toJson(adminIds),
        lastMessagePreview = lastMessagePreview,
        lastMessageAt = lastMessageAt,
        unreadCount = unreadCount,
        isPinned = isPinned,
        isMuted = isMuted,
        encryptionEnabled = encryptionEnabled,
        locality = locality
    )
}
