package com.example.offlineplayer.data.repository

import com.example.offlineplayer.data.local.MediaDao
import com.example.offlineplayer.data.local.MediaEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val mediaDao: MediaDao
) {
    val allMedia: Flow<List<MediaEntity>> = mediaDao.getAllMedia()

    suspend fun insertMedia(media: MediaEntity) = mediaDao.insertMedia(media)

    suspend fun insertMediaList(mediaList: List<MediaEntity>) = mediaDao.insertMediaList(mediaList)

    suspend fun updateMedia(media: MediaEntity) = mediaDao.updateMedia(media)

    suspend fun updateCreatorBulk(creator: String, ids: List<Int>) = mediaDao.updateCreatorBulk(creator, ids)

    suspend fun updateArtworkBulk(artworkUri: String?, ids: List<Int>) = mediaDao.updateArtworkBulk(artworkUri, ids)

    suspend fun deleteMedia(media: MediaEntity) = mediaDao.deleteMedia(media)

    suspend fun deleteMediaList(mediaIds: List<Int>) = mediaDao.deleteMediaList(mediaIds)
}