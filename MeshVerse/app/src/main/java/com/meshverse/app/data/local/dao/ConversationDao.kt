package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query(
        "SELECT * FROM conversations " +
            "WHERE isArchived = 0 " +
            "ORDER BY isPinned DESC, CASE WHEN lastMessageAt IS NULL THEN 1 ELSE 0 END, lastMessageAt DESC"
    )
    fun getAllActive(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE conversationId = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE conversationId = :id")
    fun getByIdFlow(id: String): Flow<ConversationEntity?>

    @Query("UPDATE conversations SET lastMessageId = :msgId, lastMessagePreview = :preview, lastMessageAt = :ts WHERE conversationId = :conversationId")
    suspend fun updateLastMessage(conversationId: String, msgId: String, preview: String, ts: Long)

    @Query("UPDATE conversations SET unreadCount = unreadCount + 1 WHERE conversationId = :conversationId")
    suspend fun incrementUnread(conversationId: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE conversationId = :conversationId")
    suspend fun clearUnread(conversationId: String)
}
