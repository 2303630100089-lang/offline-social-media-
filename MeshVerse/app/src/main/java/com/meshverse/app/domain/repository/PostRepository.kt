package com.meshverse.app.domain.repository

import com.meshverse.app.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getFeed(): Flow<List<Post>>
    fun getCommunityPosts(communityId: String): Flow<List<Post>>
    fun getActiveStories(): Flow<List<Post>>
    suspend fun createPost(post: Post)
    suspend fun insertPostsFromPeer(posts: List<Post>)
    suspend fun upvote(postId: String)
    suspend fun downvote(postId: String)
    suspend fun cleanExpiredPosts()
    suspend fun getUnSyncedPosts(since: Long = System.currentTimeMillis() - 86_400_000L): List<Post>
    suspend fun markPostSynced(postId: String, peerIds: String)
    suspend fun mergePostFromPeer(post: Post)
}
