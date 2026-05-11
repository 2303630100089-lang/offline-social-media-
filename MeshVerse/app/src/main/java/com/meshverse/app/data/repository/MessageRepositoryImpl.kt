package com.meshverse.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meshverse.app.data.local.dao.MessageDao
import com.meshverse.app.data.local.entity.MessageEntity
import com.meshverse.app.domain.model.Message
import com.meshverse.app.domain.model.MessageStatus
import com.meshverse.app.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val gson: Gson
) : MessageRepository {

    override fun getMessages(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun sendMessage(message: Message): Result<Unit> = runCatching {
        messageDao.insert(message.toEntity())
    }

    override suspend fun getPendingMessages(): List<Message> =
        messageDao.getPendingMessages().map { it.toDomain() }

    override suspend fun markDelivered(messageId: String) =
        messageDao.updateStatus(messageId, "delivered")

    override suspend fun markRead(conversationId: String, userId: String) =
        messageDao.markConversationRead(conversationId, userId)

    override suspend fun getByPacketId(packetId: String): Message? =
        messageDao.getByPacketId(packetId)?.toDomain()

    override suspend fun deleteSelfDestructMessages() =
        messageDao.deleteSelfDestructMessages()

    @Suppress("UNCHECKED_CAST")
    private fun MessageEntity.toDomain(): Message {
        val reactionsMap: Map<String, List<String>> = reactions?.let {
            gson.fromJson(it, object : TypeToken<Map<String, List<String>>>() {}.type)
        } ?: emptyMap()
        return Message(
            messageId = messageId,
            conversationId = conversationId,
            senderId = senderId,
            recipientId = recipientId,
            content = content,
            contentType = contentType,
            mediaPath = mediaPath,
            mediaThumbnail = mediaThumbnail,
            timestamp = timestamp,
            status = MessageStatus.valueOf(status.uppercase()),
            selfDestructAt = selfDestructAt,
            replyToMessageId = replyToMessageId,
            reactions = reactionsMap,
            hopCount = hopCount
        )
    }

    private fun Message.toEntity() = MessageEntity(
        messageId = messageId,
        conversationId = conversationId,
        senderId = senderId,
        recipientId = recipientId,
        content = content,
        contentType = contentType,
        mediaPath = mediaPath,
        mediaThumbnail = mediaThumbnail,
        timestamp = timestamp,
        status = status.name.lowercase(),
        selfDestructAt = selfDestructAt,
        replyToMessageId = replyToMessageId,
        reactions = if (reactions.isEmpty()) null else gson.toJson(reactions),
        hopCount = hopCount
    )
}
