package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Walkie-talkie room / push-to-talk channel persisted in the local database.
 */
@Entity(
    tableName = "walkie_talkie_rooms",
    indices = [
        Index("creatorId"),
        Index("channelType"),
        Index("channelNumber"),
        Index("lastActivityAt")
    ]
)
data class WalkieTalkieRoomEntity(
    @PrimaryKey val roomId: String,
    val name: String,
    val description: String = "",
    val channelType: String = "PRIVATE",
    val channelNumber: Int = 1,
    val creatorId: String,
    /** JSON list of member IDs */
    val memberIdsJson: String = "[]",
    val isEncrypted: Boolean = true,
    val isActive: Boolean = false,
    val currentSpeakerId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivityAt: Long = System.currentTimeMillis(),
    val meshAddress: String? = null
)
