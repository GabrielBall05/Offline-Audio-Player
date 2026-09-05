package com.example.offlineplayer.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.offlineplayer.data.local.MediaDao
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.util.copyUriToInternalStorage
import com.example.offlineplayer.util.getMediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val mediaDao: MediaDao,
    @param:ApplicationContext private val context: Context
) {
    val allMedia: Flow<List<MediaEntity>> = mediaDao.getAllMedia()

    suspend fun getMediaById(id: Int): MediaEntity? = withContext(Dispatchers.IO) {
        mediaDao.getMediaById(id)
    }

    //DB Actions
    suspend fun updateMedia(media: MediaEntity) = withContext(Dispatchers.IO) {
        var updatedItem = media
        media.artworkUri?.let { uri ->
            val permanentPath = copyUriToInternalStorage(context, uri.toUri())
            permanentPath?.let {
                updatedItem = media.copy(artworkUri = permanentPath)
            }
        }
        mediaDao.updateMedia(updatedItem)
    }

    suspend fun updateCreatorBulk(creator: String, ids: List<Int>) =  withContext(Dispatchers.IO) {
        mediaDao.updateCreatorBulk(creator, ids)
    }
    suspend fun updateArtworkBulk(artworkUri: String?, ids: List<Int>) = withContext(Dispatchers.IO) {
        var permanentPath = artworkUri
        artworkUri?.let { uri ->
            permanentPath = copyUriToInternalStorage(context, uri.toUri())
        }
        mediaDao.updateArtworkBulk(permanentPath, ids)
    }

    suspend fun deleteMediaList(mediaIds: List<Int>) = withContext(Dispatchers.IO) {
        mediaDao.deleteMediaList(mediaIds)
    }

    suspend fun importMedia(uriList: List<Uri>) = withContext(Dispatchers.IO) {
        val entities = uriList.mapNotNull { uri ->
            try {
                //Ensures persistent permission
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                //Extract metadata - Default MediaEntity is returned if extraction fails
                //If only specific individual metadata fields are empty, default values are placed
                getMediaMetadata(context, uri)
            } catch (e: Exception) {
                Log.e("OfflineAudioSuite", "MediaInteractor: Failed to get permission for $uri", e)
                null //Skip this one
            }
        }
        if (entities.isNotEmpty()) mediaDao.insertMediaList(entities) //Perform db insertions
    }
}