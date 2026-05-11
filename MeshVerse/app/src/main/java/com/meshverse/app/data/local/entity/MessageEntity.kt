package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index("conversationId"),
        Index("senderId"),
        Index("timestamp"),
        Index("status")
    ]
)
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val content: String,          // Encrypted ciphertext (Base64)
    val contentType: String = "text", // text, image, video, audio, file, sticker, reaction
    val mediaPath: String? = null,
    val mediaThumbnail: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val status: String = "pending", // pending, sent, delivered, read, failed
    val isEncrypted: Boolean = true,
    val selfDestructAt: Long? = null,
    val replyToMessageId: String? = null,
    val reactions: String? = null,  // JSON map of emoji -> list of userIds
    val hopCount: Int = 0,          // Mesh relay hop count
    val routePath: String? = null,  // JSON list of relay peer IDs
    val packetId: String? = null,   // For deduplication
    val isDeleted: Boolean = false
)
