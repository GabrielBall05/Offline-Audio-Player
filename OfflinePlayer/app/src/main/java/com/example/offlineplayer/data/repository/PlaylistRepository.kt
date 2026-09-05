package com.example.offlineplayer.data.repository

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.PlaylistDao
import com.example.offlineplayer.data.local.PlaylistEntity
import com.example.offlineplayer.data.local.PlaylistMediaItem
import com.example.offlineplayer.data.local.toMediaItem
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
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val controllerManager: MediaControllerManager,
    @param:ApplicationContext private val context: Context
) {
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    //Player Actions
    suspend fun playPlaylistById(id: Int, startShuffled: Boolean) {
        val mediaList = fetchPlaylistMediaList(id)
        withContext(Dispatchers.Main) { //MediaController methods must be called on the main thread
            controllerManager.playPlaylist(mediaItems = mediaList, startShuffled = startShuffled)
        }
    }

    suspend fun addPlaylistToQueue(id: Int) {
        val mediaList = fetchPlaylistMediaList(id)
        withContext(Dispatchers.Main) { //MediaController methods must be called on the main thread
            controllerManager.addToQueue(mediaList)
        }
    }

    //DB Actions
    fun getPlaylistById(id: Int): Flow<PlaylistEntity?> = playlistDao.getPlaylistById(id)

    private suspend fun fetchPlaylistMediaList(id: Int): List<MediaItem> = withContext(Dispatchers.IO) {
        playlistDao.getMediaInPlaylist(id)
            .first()
            .map { it.toMediaItem() }
    }

    suspend fun insertPlaylist(playlist: PlaylistEntity): Long = withContext(Dispatchers.IO) {
        val newPlaylist = processPlaylistCover(playlist)
        playlistDao.insertPlaylist(newPlaylist)
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        val updatedPlaylist = processPlaylistCover(playlist)
        playlistDao.updatePlaylist(updatedPlaylist)
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlist)
    }

    fun getMediaInPlaylist(playlistId: Int): Flow<List<MediaEntity>> = playlistDao.getMediaInPlaylist(playlistId)

    suspend fun getMediaNotInPlaylist(playlistId: Int): List<MediaEntity> = withContext(Dispatchers.IO) {
        playlistDao.getMediaNotInPlaylist(playlistId)
    }

    suspend fun getPlaylistsNotHavingMediaList(mediaIds: List<Int>) = withContext(Dispatchers.IO) {
        playlistDao.getPlaylistsNotHavingMediaList(mediaIds.distinct())
    }

    fun getPlaylistItemCount(playlistId: Int): Flow<Int> = playlistDao.getPlaylistItemCount(playlistId)

    suspend fun removeMediaFromPlaylist(mediaIds: List<Int>, playlistId: Int) = withContext(Dispatchers.IO) {
        playlistDao.removeMediaFromPlaylist(mediaIds, playlistId)
    }

    suspend fun moveMediaItemPositionInPlaylist(playlistId: Int, fromMediaId: Int, toMediaId: Int, fromPos: Int, toPos: Int) = withContext(Dispatchers.IO) {
        if (fromMediaId != toMediaId)
            playlistDao.moveMediaItemPositionInPlaylist(
                playlistId,
                fromMediaId,
                toMediaId,
                fromPos + 1, // + 1 because positionInPlaylist column is 1-based not 0-based
                toPos + 1
            )
    }

    //Shared Business Logic
    suspend fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) = withContext(Dispatchers.IO) {
        val allNewRefs = mutableListOf<PlaylistMediaItem>()

        //Loop through selected playlists
        playlistIds.forEach { pId ->
            //Get max position in current playlist - start at 0 if empty
            val currentMax = playlistDao.getMaxPositionInPlaylist(pId) ?: 0

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
            playlistDao.addMediaToPlaylists(allNewRefs)
        }
    }

    private fun processPlaylistCover(playlist: PlaylistEntity): PlaylistEntity {
        var updatedPlaylist = playlist
        playlist.coverUri?.let { uri ->
            val permanentPath = copyUriToInternalStorage(context, uri.toUri())
            permanentPath?.let {
                updatedPlaylist = playlist.copy(coverUri = it)
            }
        }
        return updatedPlaylist
    }















    //suspend fun insertPlaylist(playlist: PlaylistEntity): Long = playlistDao.insertPlaylist(playlist)

    //fun getPlaylistById(id: Int): Flow<PlaylistEntity?> = playlistDao.getPlaylistById(id)

    //suspend fun updatePlaylist(playlist: PlaylistEntity) = playlistDao.updatePlaylist(playlist)

    //suspend fun deletePlaylist(playlist: PlaylistEntity) = playlistDao.deletePlaylist(playlist)

    //suspend fun addMediaToPlaylists(items: List<PlaylistMediaItem>) = playlistDao.addMediaToPlaylists(items)

    //fun getAllMediaInPlaylist(playlistId: Int): Flow<List<MediaEntity>> = playlistDao.getMediaInPlaylist(playlistId)

    //suspend fun getMediaNotInPlaylist(playlistId: Int): List<MediaEntity> = playlistDao.getMediaNotInPlaylist(playlistId)

    //suspend fun getPlaylistsNotHavingMediaList(mediaIds: List<Int>) = playlistDao.getPlaylistsNotHavingMediaList(mediaIds.distinct())

    //fun getPlaylistItemCount(playlistId: Int): Flow<Int> = playlistDao.getPlaylistItemCount(playlistId)

//    suspend fun moveMediaItemPositionInPlaylist(playlistId: Int, fromMediaId: Int, toMediaId: Int, fromPos: Int, toPos: Int)
//        = playlistDao.moveMediaItemPositionInPlaylist(playlistId, fromMediaId, toMediaId, fromPos + 1, toPos + 1)
//        // + 1 because positionInPlaylist column is 1-based not 0-based

    //suspend fun removeMediaFromPlaylist(mediaIds: List<Int>, playlistId: Int) = playlistDao.removeMediaFromPlaylist(mediaIds, playlistId)

    //suspend fun getMaxPositionInPlaylist(playlistId: Int): Int? = playlistDao.getMaxPositionInPlaylist(playlistId)
}
