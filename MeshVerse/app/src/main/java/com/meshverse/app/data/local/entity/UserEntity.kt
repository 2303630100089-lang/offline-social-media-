package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val displayName: String,
    val avatarPath: String?,
    val publicKey: String,        // Base64 Curve25519 public key
    val deviceFingerprint: String,
    val isLocalUser: Boolean = false,
    val isAnonymous: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val reputation: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val bio: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
