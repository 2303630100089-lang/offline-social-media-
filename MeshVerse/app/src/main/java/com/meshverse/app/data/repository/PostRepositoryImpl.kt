package com.meshverse.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meshverse.app.data.local.dao.PostDao
import com.meshverse.app.data.local.entity.PostEntity
import com.meshverse.app.domain.model.Post
import com.meshverse.app.domain.model.PostType
import com.meshverse.app.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val postDao: PostDao,
    private val gson: Gson
) : PostRepository {

    override fun getFeed(): Flow<List<Post>> =
        postDao.getFeed().map { it.map { e -> e.toDomain() } }

    override fun getCommunityPosts(communityId: String): Flow<List<Post>> =
        postDao.getCommunityPosts(communityId).map { it.map { e -> e.toDomain() } }

    override fun getActiveStories(): Flow<List<Post>> =
        postDao.getActiveStories().map { it.map { e -> e.toDomain() } }

    override suspend fun createPost(post: Post) =
        postDao.insert(post.toEntity())

    override suspend fun insertPostsFromPeer(posts: List<Post>) =
        postDao.insertAll(posts.map { it.toEntity() })

    override suspend fun upvote(postId: String) = postDao.upvote(postId)
    override suspend fun downvote(postId: String) = postDao.downvote(postId)
    override suspend fun cleanExpiredPosts() = postDao.deleteExpiredPosts()

    private fun PostEntity.toDomain(): Post {
        val tagList: List<String> = tags?.let {
            gson.fromJson(it, object : TypeToken<List<String>>() {}.type)
        } ?: emptyList()
        return Post(
            postId = postId, authorId = authorId, communityId = communityId,
            title = title, content = content,
            postType = runCatching { PostType.valueOf(postType.uppercase()) }.getOrDefault(PostType.POST),
            mediaPath = mediaPath, mediaThumbnail = mediaThumbnail,
            tags = tagList, upvotes = upvotes, downvotes = downvotes,
            commentCount = commentCount, shareCount = shareCount,
            locality = locality, latitude = latitude, longitude = longitude,
            timestamp = timestamp, expiresAt = expiresAt
        )
    }

    private fun Post.toEntity() = PostEntity(
        postId = postId, authorId = authorId, communityId = communityId,
        title = title, content = content,
        postType = postType.name.lowercase(),
        mediaPath = mediaPath, mediaThumbnail = mediaThumbnail,
        tags = if (tags.isEmpty()) null else gson.toJson(tags),
        upvotes = upvotes, downvotes = downvotes,
        commentCount = commentCount, shareCount = shareCount,
        locality = locality, latitude = latitude, longitude = longitude,
        timestamp = timestamp, expiresAt = expiresAt
    )
}
