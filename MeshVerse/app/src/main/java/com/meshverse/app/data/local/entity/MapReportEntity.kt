package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Community-generated map report stored locally and propagated over mesh.
 */
@Entity(
    tableName = "map_reports",
    indices = [
        Index("authorId"),
        Index("category"),
        Index("timestamp"),
        Index("expiresAt")
    ]
)
data class MapReportEntity(
    @PrimaryKey val reportId: String,
    val authorId: String,
    val category: String,
    val title: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
    val upvotes: Int = 0,
    val isVerified: Boolean = false,
    val propagationHops: Int = 0,
    val isSynced: Boolean = false
)
