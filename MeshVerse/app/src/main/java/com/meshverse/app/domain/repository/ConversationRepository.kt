package com.meshverse.app.domain.repository

import com.meshverse.app.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun getAllConversations(): Flow<List<Conversation>>
    suspend fun getConversationById(id: String): Conversation?
    suspend fun createConversation(conversation: Conversation)
    suspend fun updateLastMessage(conversationId: String, preview: String, timestamp: Long)
    suspend fun markRead(conversationId: String)
}
