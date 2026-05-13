package com.meshverse.app.domain.repository

import com.meshverse.app.domain.model.Comment
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun getCommentsForPost(postId: String): Flow<List<Comment>>
    fun getReplies(parentCommentId: String): Flow<List<Comment>>
    suspend fun addComment(comment: Comment)
    suspend fun upvote(commentId: String)
    suspend fun downvote(commentId: String)
    suspend fun deleteComment(commentId: String)
}
