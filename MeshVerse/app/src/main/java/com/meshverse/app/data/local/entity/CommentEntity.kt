package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Comment or nested reply stored locally.
 * parentCommentId = null for top-level comments on a post.
 */
@Entity(
    tableName = "comments",
    indices = [
        Index("postId"),
        Index("authorId"),
        Index("parentCommentId"),
        Index("timestamp")
    ]
)
data class CommentEntity(
    @PrimaryKey val commentId: String,
    val postId: String,
    val authorId: String,
    val authorName: String = "Anonymous",
    val content: String,
    /** null = top-level comment; non-null = reply to another comment */
    val parentCommentId: String? = null,
    val depth: Int = 0,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false
)
