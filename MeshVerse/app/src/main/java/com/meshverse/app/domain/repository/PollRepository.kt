package com.meshverse.app.domain.repository

import com.meshverse.app.domain.model.Poll
import com.meshverse.app.domain.model.PollOption
import kotlinx.coroutines.flow.Flow

interface PollRepository {
    fun getAllPolls(): Flow<List<Poll>>
    fun getPollsForConversation(conversationId: String): Flow<List<Poll>>
    suspend fun getPollById(pollId: String): Poll?
    suspend fun createPoll(poll: Poll)
    suspend fun vote(pollId: String, optionId: String, voterId: String)
    suspend fun mergePollFromPeer(poll: Poll)
    suspend fun getUnSyncedPolls(): List<Poll>
    suspend fun markSynced(pollId: String)
    suspend fun cleanExpiredPolls()
}
