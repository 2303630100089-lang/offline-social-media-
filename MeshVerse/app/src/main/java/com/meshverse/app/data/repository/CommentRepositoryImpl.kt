package com.meshverse.app.data.repository

import com.meshverse.app.data.local.dao.CommentDao
import com.meshverse.app.data.local.entity.CommentEntity
import com.meshverse.app.domain.model.Comment
import com.meshverse.app.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
    private val commentDao: CommentDao
) : CommentRepository {

    override fun getCommentsForPost(postId: String): Flow<List<Comment>> =
        commentDao.getAllCommentsForPost(postId).map { list -> list.map { it.toDomain() } }

    override fun getReplies(parentCommentId: String): Flow<List<Comment>> =
        commentDao.getReplies(parentCommentId).map { list -> list.map { it.toDomain() } }

    override suspend fun addComment(comment: Comment) =
        commentDao.insert(comment.toEntity())

    override suspend fun upvote(commentId: String) = commentDao.upvote(commentId)
    override suspend fun downvote(commentId: String) = commentDao.downvote(commentId)
    override suspend fun deleteComment(commentId: String) = commentDao.softDelete(commentId)

    private fun CommentEntity.toDomain() = Comment(
        commentId = commentId, postId = postId, authorId = authorId,
        authorName = authorName, content = content,
        parentCommentId = parentCommentId, depth = depth,
        upvotes = upvotes, downvotes = downvotes, timestamp = timestamp
    )

    private fun Comment.toEntity() = CommentEntity(
        commentId = commentId, postId = postId, authorId = authorId,
        authorName = authorName, content = content,
        parentCommentId = parentCommentId, depth = depth,
        upvotes = upvotes, downvotes = downvotes, timestamp = timestamp
    )
}
