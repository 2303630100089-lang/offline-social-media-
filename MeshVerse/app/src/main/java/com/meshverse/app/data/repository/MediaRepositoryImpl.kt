package com.meshverse.app.data.repository

import com.meshverse.app.data.local.dao.MediaDao
import com.meshverse.app.data.local.entity.MediaEntity
import com.meshverse.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val mediaDao: MediaDao
) : MediaRepository {

    override fun getMediaByUser(userId: String): Flow<List<MediaEntity>> =
        mediaDao.getMediaByUser(userId)

    override suspend fun saveMedia(media: MediaEntity) =
        mediaDao.insert(media)

    override suspend fun updateDownloadProgress(mediaId: String, progress: Int, done: Boolean) =
        mediaDao.updateDownloadProgress(mediaId, progress, done)

    override suspend fun getById(mediaId: String): MediaEntity? =
        mediaDao.getById(mediaId)
}
