package com.meshverse.app.domain.model

import java.util.UUID

/**
 * A walkie-talkie room / push-to-talk channel.
 *
 * Channel types mirror different real-world use cases:
 *  - PRIVATE  : invite-only encrypted voice group (family, friends)
 *  - COMMUNITY: open neighbourhood / campus channel
 *  - EMERGENCY: always-on emergency override channel (highest priority)
 *  - EVENT    : temporary event channel
 *  - GAMING   : low-latency squad comms
 */
data class WalkieTalkieRoom(
    val roomId: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val channelType: ChannelType = ChannelType.PRIVATE,
    val channelNumber: Int = 1,           // "Radio station" number (1–99)
    val creatorId: String,
    val memberIds: List<String> = emptyList(),
    val isEncrypted: Boolean = true,
    val isActive: Boolean = false,
    val currentSpeakerId: String? = null,  // Who is currently transmitting
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivityAt: Long = System.currentTimeMillis(),
    /** Mesh broadcast address for this room (null = room creator's nodeId used as scope) */
    val meshAddress: String? = null
)

enum class ChannelType {
    PRIVATE,
    COMMUNITY,
    EMERGENCY,
    EVENT,
    GAMING
}
