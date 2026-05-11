package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.PollEntity
import com.meshverse.app.data.local.entity.PollVoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PollDao {

    // ── Polls ──────────────────────────────────────────────────────────────

    @Upsert
    suspend fun upsertPoll(poll: PollEntity)

    @Query("SELECT * FROM polls ORDER BY createdAt DESC")
    fun getAllPolls(): Flow<List<PollEntity>>

    @Query("SELECT * FROM polls WHERE conversationId = :conversationId ORDER BY createdAt DESC")
    fun getPollsForConversation(conversationId: String): Flow<List<PollEntity>>

    @Query("SELECT * FROM polls WHERE communityId = :communityId ORDER BY createdAt DESC")
    fun getPollsForCommunity(communityId: String): Flow<List<PollEntity>>

    @Query("SELECT * FROM polls WHERE pollId = :pollId LIMIT 1")
    suspend fun getPollById(pollId: String): PollEntity?

    @Query("SELECT * FROM polls WHERE isSynced = 0")
    suspend fun getUnSyncedPolls(): List<PollEntity>

    @Query("UPDATE polls SET isSynced = 1 WHERE pollId = :pollId")
    suspend fun markSynced(pollId: String)

    @Query("UPDATE polls SET totalVotes = totalVotes + 1, version = version + 1 WHERE pollId = :pollId")
    suspend fun incrementVoteCount(pollId: String)

    @Query("UPDATE polls SET optionsJson = :optionsJson, totalVotes = :totalVotes, version = :version WHERE pollId = :pollId AND version < :version")
    suspend fun mergePollUpdate(pollId: String, optionsJson: String, totalVotes: Int, version: Long)

    @Query("DELETE FROM polls WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpiredPolls(now: Long = System.currentTimeMillis())

    // ── Votes ──────────────────────────────────────────────────────────────

    @Upsert
    suspend fun upsertVote(vote: PollVoteEntity)

    @Query("SELECT * FROM poll_votes WHERE pollId = :pollId")
    fun getVotesForPoll(pollId: String): Flow<List<PollVoteEntity>>

    @Query("SELECT * FROM poll_votes WHERE pollId = :pollId AND voterId = :voterId LIMIT 1")
    suspend fun getUserVote(pollId: String, voterId: String): PollVoteEntity?

    @Query("SELECT * FROM poll_votes WHERE isSynced = 0")
    suspend fun getUnSyncedVotes(): List<PollVoteEntity>

    @Query("UPDATE poll_votes SET isSynced = 1 WHERE voteId = :voteId")
    suspend fun markVoteSynced(voteId: String)
}
