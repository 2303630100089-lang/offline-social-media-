package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts",
    indices = [
        Index("authorId"),
        Index("communityId"),
        Index("timestamp"),
        Index("postType")
    ]
)
data class PostEntity(
    @PrimaryKey val postId: String,
    val authorId: String,
    val communityId: String? = null,
    val title: String? = null,
    val content: String,
    val postType: String = "post", // post, story, reel, thread, meme, announcement
    val mediaPath: String? = null,
    val mediaThumbnail: String? = null,
    val tags: String? = null,       // JSON list of hashtags
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val viewCount: Int = 0,
    val locality: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,    // For stories (24h)
    val isPinned: Boolean = false,
    val isNSFW: Boolean = false,
    val propagationRadius: Float = 5000f, // meters
    val syncedPeerIds: String? = null    // JSON list of peer IDs that received this
)
