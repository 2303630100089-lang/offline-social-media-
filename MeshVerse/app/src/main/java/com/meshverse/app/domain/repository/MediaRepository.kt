package com.meshverse.app.domain.repository

import com.meshverse.app.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMediaByUser(userId: String): Flow<List<MediaEntity>>
    suspend fun saveMedia(media: MediaEntity)
    suspend fun updateDownloadProgress(mediaId: String, progress: Int, done: Boolean)
    suspend fun getById(mediaId: String): MediaEntity?
}
