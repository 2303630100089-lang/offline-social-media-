package com.meshverse.app.domain.model

data class Post(
    val postId: String,
    val authorId: String,
    val author: User? = null,
    val communityId: String? = null,
    val title: String? = null,
    val content: String,
    val postType: PostType = PostType.POST,
    val mediaPath: String? = null,
    val mediaThumbnail: String? = null,
    val tags: List<String> = emptyList(),
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val locality: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
)

enum class PostType { POST, STORY, REEL, THREAD, MEME, ANNOUNCEMENT }
