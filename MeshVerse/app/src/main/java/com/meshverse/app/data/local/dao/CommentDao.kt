package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity)

    @Query("SELECT * FROM comments WHERE postId = :postId AND parentCommentId IS NULL ORDER BY timestamp ASC")
    fun getTopLevelComments(postId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE parentCommentId = :parentCommentId ORDER BY timestamp ASC")
    fun getReplies(parentCommentId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getAllCommentsForPost(postId: String): Flow<List<CommentEntity>>

    @Query("UPDATE comments SET upvotes = upvotes + 1 WHERE commentId = :commentId")
    suspend fun upvote(commentId: String)

    @Query("UPDATE comments SET downvotes = downvotes + 1 WHERE commentId = :commentId")
    suspend fun downvote(commentId: String)

    @Query("UPDATE comments SET isDeleted = 1, content = '[deleted]' WHERE commentId = :commentId")
    suspend fun softDelete(commentId: String)

    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId AND isDeleted = 0")
    suspend fun countForPost(postId: String): Int

    @Query("SELECT * FROM comments WHERE isSynced = 0")
    suspend fun getUnsynced(): List<CommentEntity>

    @Query("UPDATE comments SET isSynced = 1 WHERE commentId = :commentId")
    suspend fun markSynced(commentId: String)
}
