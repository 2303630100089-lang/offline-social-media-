package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Encrypted poll stored in the local Room database.
 * Poll options are serialised as a JSON string to avoid a separate join table.
 */
@Entity(
    tableName = "polls",
    indices = [
        Index("authorId"),
        Index("conversationId"),
        Index("createdAt"),
        Index("expiresAt")
    ]
)
data class PollEntity(
    @PrimaryKey val pollId: String,
    val authorId: String,
    val conversationId: String? = null,
    val communityId: String? = null,
    val question: String,
    /** JSON: List<PollOptionJson> */
    val optionsJson: String,
    val pollType: String = "PUBLIC",
    val isAnonymous: Boolean = false,
    val isMultiChoice: Boolean = false,
    val isQuizMode: Boolean = false,
    val correctOptionIndex: Int? = null,
    val totalVotes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    /** Lamport clock for CRDT merge */
    val version: Long = 0L,
    val isSynced: Boolean = false
)
