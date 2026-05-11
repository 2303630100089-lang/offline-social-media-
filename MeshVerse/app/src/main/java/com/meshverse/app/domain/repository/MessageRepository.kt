package com.meshverse.app.domain.repository

import com.meshverse.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessages(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Result<Unit>
    suspend fun getPendingMessages(): List<Message>
    suspend fun markDelivered(messageId: String)
    suspend fun markRead(conversationId: String, userId: String)
    suspend fun getByPacketId(packetId: String): Message?
    suspend fun deleteSelfDestructMessages()
}
