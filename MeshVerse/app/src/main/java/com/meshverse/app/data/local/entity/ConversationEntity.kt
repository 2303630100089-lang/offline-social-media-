package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val type: String = "direct",    // direct, group, channel, community, event
    val name: String?,
    val description: String? = null,
    val avatarPath: String? = null,
    val participantIds: String,     // JSON list
    val adminIds: String? = null,   // JSON list for groups
    val lastMessageId: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageAt: Long? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val encryptionEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val locality: String? = null,   // Geographic locality tag
    val maxHops: Int = 5            // Max relay hops for this conversation
)
