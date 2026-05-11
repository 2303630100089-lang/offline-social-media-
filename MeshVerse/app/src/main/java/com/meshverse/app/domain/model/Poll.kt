package com.meshverse.app.domain.model

import java.util.UUID

/**
 * A distributed poll that synchronises across the mesh network.
 * Results propagate via POLL_SYNC packets using CRDT-like LWW semantics:
 * each vote is timestamped and the latest timestamp wins on conflict.
 */
data class Poll(
    val pollId: String = UUID.randomUUID().toString(),
    val authorId: String,
    val conversationId: String? = null,
    val communityId: String? = null,
    val question: String,
    val options: List<PollOption>,
    val pollType: PollType = PollType.PUBLIC,
    val isAnonymous: Boolean = false,
    val isMultiChoice: Boolean = false,
    val isQuizMode: Boolean = false,
    val correctOptionIndex: Int? = null,
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val totalVotes: Int = 0,
    /** Lamport clock for CRDT conflict resolution */
    val version: Long = 0L
)

data class PollOption(
    val optionId: String = UUID.randomUUID().toString(),
    val text: String,
    val voteCount: Int = 0
)

enum class PollType {
    PUBLIC,       // Results visible to all
    ANONYMOUS,    // Votes are anonymous
    QUIZ,         // Has a correct answer
    LOCAL_VOTE,   // Community governance vote
    EMERGENCY     // Emergency decision poll
}
