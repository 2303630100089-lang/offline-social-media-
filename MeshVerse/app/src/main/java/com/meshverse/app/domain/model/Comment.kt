package com.meshverse.app.domain.model

data class Comment(
    val commentId: String,
    val postId: String,
    val authorId: String,
    val authorName: String = "Anonymous",
    val content: String,
    /** null = top-level comment; non-null = reply thread */
    val parentCommentId: String? = null,
    val depth: Int = 0,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val replies: List<Comment> = emptyList()
)
