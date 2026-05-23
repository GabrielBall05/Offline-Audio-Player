package com.example.offlineplayer.data.domain

import android.content.Context
import androidx.core.net.toUri
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.PlaylistEntity
import com.example.offlineplayer.data.local.PlaylistMediaItem
import com.example.offlineplayer.data.local.toMediaItem
import com.example.offlineplayer.data.repository.PlaylistRepository
import com.example.offlineplayer.player.MediaControllerManager
import com.example.offlineplayer.util.copyUriToInternalStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistInteractor @Inject constructor(
    private val repository: PlaylistRepository,
    private val controllerManager: MediaControllerManager,
    @param:ApplicationContext private val context: Context
) {
    //Shared Flow
    val allPlaylists = repository.allPlaylists

    //Player Actions
    suspend fun playPlaylistById(id: Int) {
        //Perform DB operation on IO thread
        val mediaList = withContext(Dispatchers.IO) {
            repository.getAllMediaInPlaylist(id)
                .first()
                .map { it.toMediaItem() }
        }
        //MediaController methods must be called on the main thread
        controllerManager.playPlaylist(mediaList)
    }

    //DB Actions
    fun getPlaylistById(id: Int): Flow<PlaylistEntity?> = repository.getPlaylistById(id)
    suspend fun createPlaylist(playlist: PlaylistEntity): Long {
        val newPlaylist = processPlaylistCover(playlist)
        return repository.insertPlaylist(newPlaylist)
    }
    suspend fun editPlaylist(playlist: PlaylistEntity) {
        val updatedPlaylist = processPlaylistCover(playlist)
        repository.updatePlaylist(updatedPlaylist)
    }
    suspend fun deletePlaylist(playlist: PlaylistEntity) = repository.deletePlaylist(playlist)
    fun getMediaInPlaylist(playlistId: Int): Flow<List<MediaEntity>> = repository.getAllMediaInPlaylist(playlistId)
    suspend fun getMediaNotInPlaylist(playlistId: Int): List<MediaEntity> = repository.getMediaNotInPlaylist(playlistId)
    suspend fun getPlaylistsNotHavingMediaList(mediaIds: List<Int>) = repository.getPlaylistsNotHavingMediaList(mediaIds)
    fun getPlaylistItemCount(playlistId: Int): Flow<Int> = repository.getPlaylistItemCount(playlistId)
    suspend fun removeMediaFromPlaylist(mediaIds: List<Int>, playlistId: Int) = repository.removeMediaFromPlaylist(mediaIds, playlistId)

    //Shared Business Logic
    suspend fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) {
        val allNewRefs = mutableListOf<PlaylistMediaItem>()

        //Loop through selected playlists
        playlistIds.forEach { pId ->
            //Get max position in current playlist - start at 0 if empty
            val currentMax = repository.getMaxPositionInPlaylist(pId) ?: 0

            //Make a PlaylistMediaItem out of all selected media items and the current playlist
            val playlistRefs = mediaIds.mapIndexed { index, mId ->
                PlaylistMediaItem(
                    playlistId = pId,
                    mediaId = mId,
                    positionInPlaylist = currentMax + index + 1 //Ensures proper incrementing
                )
            }
            allNewRefs.addAll(playlistRefs)
        }

        //Insert all items into all playlists
        if (allNewRefs.isNotEmpty()) {
            repository.addMediaToPlaylists(allNewRefs)
        }
    }

    private fun processPlaylistCover(playlist: PlaylistEntity): PlaylistEntity {
        var updatedPlaylist = playlist
        playlist.coverImage?.let { uri ->
            val permanentPath = copyUriToInternalStorage(context, uri.toUri())
            permanentPath?.let {
                updatedPlaylist = playlist.copy(coverImage = it)
            }
        }
        return updatedPlaylist
    }
}