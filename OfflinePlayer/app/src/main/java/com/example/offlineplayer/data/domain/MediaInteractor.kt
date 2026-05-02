package com.example.offlineplayer.data.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.repository.MediaRepository
import com.example.offlineplayer.player.MediaControllerManager
import com.example.offlineplayer.util.getMediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaInteractor @Inject constructor(
    private val repository: MediaRepository,
    private val controllerManager: MediaControllerManager,
    @param:ApplicationContext private val context: Context
) {
    //Shared Flow
    val allMedia = repository.allMedia

    //Player Actions
    fun playMedia(media: MediaEntity) = controllerManager.playNow(media)
    fun addMediaToQueue(media: MediaEntity) = controllerManager.addToQueue(media)

    //DB Actions
    suspend fun updateMedia(media: MediaEntity) = repository.updateMedia(media)
    suspend fun updateCreatorBulk(creator: String, ids: List<Int>) = repository.updateCreatorBulk(creator, ids)
    suspend fun updateArtworkBulk(artworkUri: String?, ids: List<Int>) = repository.updateArtworkBulk(artworkUri, ids)
    suspend fun deleteMedia(media: MediaEntity) = repository.deleteMedia(media)
    suspend fun deleteMediaList(mediaIds: List<Int>) = repository.deleteMediaList(mediaIds)
    suspend fun insertMedia(media: MediaEntity) = repository.insertMedia(media)

    //Shared Business Logic
    suspend fun importMedia(uriList: List<Uri>) {
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
        if (entities.isNotEmpty()) repository.insertMediaList(entities) //Perform db insertions
    }
}
