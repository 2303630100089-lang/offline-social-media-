package com.meshverse.app.domain.model

data class Conversation(
    val conversationId: String,
    val type: ConversationType = ConversationType.DIRECT,
    val name: String?,
    val description: String? = null,
    val avatarPath: String? = null,
    val participantIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val lastMessagePreview: String? = null,
    val lastMessageAt: Long? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val encryptionEnabled: Boolean = true,
    val locality: String? = null
)

enum class ConversationType { DIRECT, GROUP, CHANNEL, COMMUNITY, EVENT }
