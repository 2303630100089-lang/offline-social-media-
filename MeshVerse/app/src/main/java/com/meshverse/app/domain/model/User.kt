package com.meshverse.app.domain.model

data class User(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarPath: String?,
    val publicKey: String,
    val deviceFingerprint: String,
    val isLocalUser: Boolean = false,
    val isAnonymous: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val reputation: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val bio: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0
)
