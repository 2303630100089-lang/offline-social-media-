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
}
