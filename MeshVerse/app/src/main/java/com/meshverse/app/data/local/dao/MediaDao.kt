package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaEntity)

    @Update
    suspend fun update(media: MediaEntity)

    @Query("SELECT * FROM media WHERE ownerId = :userId ORDER BY createdAt DESC")
    fun getMediaByUser(userId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE mediaId = :mediaId")
    suspend fun getById(mediaId: String): MediaEntity?

    @Query("UPDATE media SET downloadProgress = :progress, isDownloaded = :done WHERE mediaId = :mediaId")
    suspend fun updateDownloadProgress(mediaId: String, progress: Int, done: Boolean)
}
