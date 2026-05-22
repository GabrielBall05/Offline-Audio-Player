package com.example.offlineplayer.data.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.toMediaItem
import com.example.offlineplayer.data.repository.MediaRepository
import com.example.offlineplayer.player.MediaControllerManager
import com.example.offlineplayer.util.copyUriToInternalStorage
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
    fun playMedia(media: MediaEntity) = controllerManager.playNow(media.toMediaItem())
    fun addMediaToQueue(media: MediaEntity) = controllerManager.addToQueue(media.toMediaItem())

    //DB Actions
    suspend fun updateMedia(media: MediaEntity) {
        var updatedItem = media
        media.artworkUri?.let { uri ->
            val permanentPath = copyUriToInternalStorage(context, uri.toUri())
            permanentPath?.let {
                updatedItem = media.copy(artworkUri = permanentPath)
            }
        }
        repository.updateMedia(updatedItem)
    }

    suspend fun updateCreatorBulk(creator: String, ids: List<Int>) = repository.updateCreatorBulk(creator, ids)
    suspend fun updateArtworkBulk(artworkUri: String?, ids: List<Int>) {
        var permanentPath = artworkUri
        artworkUri?.let { uri ->
            permanentPath = copyUriToInternalStorage(context, uri.toUri())
        }
        repository.updateArtworkBulk(permanentPath, ids)
    }

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
