package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Individual vote cast on a poll option.
 * Kept separate from PollEntity to support anonymous aggregation
 * and per-device deduplication.
 */
@Entity(
    tableName = "poll_votes",
    indices = [
        Index(value = ["pollId", "voterId"], unique = true),
        Index("pollId"),
        Index("optionId")
    ]
)
data class PollVoteEntity(
    @PrimaryKey val voteId: String,
    val pollId: String,
    val optionId: String,
    /** Null when poll is anonymous and vote was cast by a remote node. */
    val voterId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
