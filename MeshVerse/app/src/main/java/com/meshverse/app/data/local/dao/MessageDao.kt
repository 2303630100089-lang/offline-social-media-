package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isDeleted = 0 ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestMessages(conversationId: String, limit: Int = 50): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE status = 'pending' ORDER BY timestamp ASC")
    suspend fun getPendingMessages(): List<MessageEntity>

    @Query("UPDATE messages SET status = :status, deliveredAt = :timestamp WHERE messageId = :messageId")
    suspend fun updateStatus(messageId: String, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE messages SET readAt = :timestamp, status = 'read' WHERE conversationId = :conversationId AND recipientId = :userId AND readAt IS NULL")
    suspend fun markConversationRead(conversationId: String, userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND readAt IS NULL AND senderId != :localUserId")
    fun getUnreadCount(conversationId: String, localUserId: String): Flow<Int>

    @Query("DELETE FROM messages WHERE selfDestructAt IS NOT NULL AND selfDestructAt < :now")
    suspend fun deleteSelfDestructMessages(now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM messages WHERE packetId = :packetId LIMIT 1")
    suspend fun getByPacketId(packetId: String): MessageEntity?

    @Query("UPDATE messages SET isDeleted = 1 WHERE messageId = :messageId")
    suspend fun softDelete(messageId: String)
}
