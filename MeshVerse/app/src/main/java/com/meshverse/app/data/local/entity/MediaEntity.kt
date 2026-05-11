package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val mediaId: String,
    val ownerId: String,
    val fileName: String,
    val mimeType: String,
    val localPath: String,
    val thumbnailPath: String? = null,
    val fileSize: Long = 0,
    val duration: Long? = null,     // For audio/video in ms
    val width: Int? = null,
    val height: Int? = null,
    val checksum: String? = null,   // SHA-256
    val isDownloaded: Boolean = false,
    val downloadProgress: Int = 0,
    val sourceType: String = "local", // local, received, cached
    val sharedWithPeers: String? = null, // JSON list
    val createdAt: Long = System.currentTimeMillis()
)
