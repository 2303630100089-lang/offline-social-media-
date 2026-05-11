package com.meshverse.app.domain.model

data class Message(
    val messageId: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val content: String,           // Decrypted plaintext
    val contentType: String = "text",
    val mediaPath: String? = null,
    val mediaThumbnail: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.PENDING,
    val selfDestructAt: Long? = null,
    val replyToMessageId: String? = null,
    val reactions: Map<String, List<String>> = emptyMap(),
    val hopCount: Int = 0,
    val isOwn: Boolean = false
)

enum class MessageStatus {
    PENDING, SENT, DELIVERED, READ, FAILED
}
