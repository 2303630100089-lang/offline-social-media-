package com.meshverse.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meshverse.app.data.local.dao.PollDao
import com.meshverse.app.data.local.entity.PollEntity
import com.meshverse.app.data.local.entity.PollVoteEntity
import com.meshverse.app.domain.model.Poll
import com.meshverse.app.domain.model.PollOption
import com.meshverse.app.domain.model.PollType
import com.meshverse.app.domain.repository.PollRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class PollRepositoryImpl @Inject constructor(
    private val pollDao: PollDao,
    private val gson: Gson
) : PollRepository {

    override fun getAllPolls(): Flow<List<Poll>> =
        pollDao.getAllPolls().map { it.map { e -> e.toDomain() } }

    override fun getPollsForConversation(conversationId: String): Flow<List<Poll>> =
        pollDao.getPollsForConversation(conversationId).map { it.map { e -> e.toDomain() } }

    override suspend fun getPollById(pollId: String): Poll? =
        pollDao.getPollById(pollId)?.toDomain()

    override suspend fun createPoll(poll: Poll) =
        pollDao.upsertPoll(poll.toEntity())

    override suspend fun vote(pollId: String, optionId: String, voterId: String) {
        // Idempotent: upsert handles duplicate detection
        val vote = PollVoteEntity(
            voteId = "$pollId-$voterId",
            pollId = pollId,
            optionId = optionId,
            voterId = voterId
        )
        pollDao.upsertVote(vote)
        pollDao.incrementVoteCount(pollId)

        // Update option vote count in JSON
        val entity = pollDao.getPollById(pollId) ?: return
        val options = deserializeOptions(entity.optionsJson).map { opt ->
            if (opt.optionId == optionId) opt.copy(voteCount = opt.voteCount + 1) else opt
        }
        pollDao.upsertPoll(entity.copy(optionsJson = gson.toJson(options), version = entity.version + 1))
    }

    /**
     * CRDT-style merge: only applies if incoming version is newer.
     */
    override suspend fun mergePollFromPeer(poll: Poll) {
        val existing = pollDao.getPollById(poll.pollId)
        if (existing == null) {
            pollDao.upsertPoll(poll.toEntity())
        } else {
            pollDao.mergePollUpdate(
                pollId = poll.pollId,
                optionsJson = gson.toJson(poll.options),
                totalVotes = poll.totalVotes,
                version = poll.version
            )
        }
    }

    override suspend fun getUnSyncedPolls(): List<Poll> =
        pollDao.getUnSyncedPolls().map { it.toDomain() }

    override suspend fun markSynced(pollId: String) = pollDao.markSynced(pollId)

    override suspend fun cleanExpiredPolls() = pollDao.deleteExpiredPolls()

    // ── Mapping helpers ────────────────────────────────────────────────────

    private fun PollEntity.toDomain(): Poll = Poll(
        pollId = pollId,
        authorId = authorId,
        conversationId = conversationId,
        communityId = communityId,
        question = question,
        options = deserializeOptions(optionsJson),
        pollType = runCatching { PollType.valueOf(pollType) }.getOrDefault(PollType.PUBLIC),
        isAnonymous = isAnonymous,
        isMultiChoice = isMultiChoice,
        isQuizMode = isQuizMode,
        correctOptionIndex = correctOptionIndex,
        expiresAt = expiresAt,
        createdAt = createdAt,
        totalVotes = totalVotes,
        version = version
    )

    private fun Poll.toEntity() = PollEntity(
        pollId = pollId,
        authorId = authorId,
        conversationId = conversationId,
        communityId = communityId,
        question = question,
        optionsJson = gson.toJson(options),
        pollType = pollType.name,
        isAnonymous = isAnonymous,
        isMultiChoice = isMultiChoice,
        isQuizMode = isQuizMode,
        correctOptionIndex = correctOptionIndex,
        totalVotes = totalVotes,
        createdAt = createdAt,
        expiresAt = expiresAt,
        version = version,
        isSynced = false
    )

    private fun deserializeOptions(json: String): List<PollOption> =
        runCatching {
            gson.fromJson<List<PollOption>>(json, object : TypeToken<List<PollOption>>() {}.type)
        }.getOrDefault(emptyList())
}
