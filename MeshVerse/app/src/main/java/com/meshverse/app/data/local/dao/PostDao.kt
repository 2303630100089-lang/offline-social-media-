package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Update
    suspend fun update(post: PostEntity)

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getFeed(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE communityId = :communityId ORDER BY timestamp DESC")
    fun getCommunityPosts(communityId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE postType = 'story' AND (expiresAt IS NULL OR expiresAt > :now) ORDER BY timestamp DESC")
    fun getActiveStories(now: Long = System.currentTimeMillis()): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE tags LIKE '%' || :tag || '%' ORDER BY timestamp DESC")
    fun getPostsByTag(tag: String): Flow<List<PostEntity>>

    @Query("UPDATE posts SET upvotes = upvotes + 1 WHERE postId = :postId")
    suspend fun upvote(postId: String)

    @Query("UPDATE posts SET downvotes = downvotes + 1 WHERE postId = :postId")
    suspend fun downvote(postId: String)

    @Query("DELETE FROM posts WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpiredPosts(now: Long = System.currentTimeMillis())
}
