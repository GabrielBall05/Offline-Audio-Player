package com.example.offlineplayer.data.repository

import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.PlaylistDao
import com.example.offlineplayer.data.local.PlaylistEntity
import com.example.offlineplayer.data.local.PlaylistMediaItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun insertPlaylist(playlist: PlaylistEntity): Long = playlistDao.insertPlaylist(playlist)

    fun getPlaylistById(id: Int): Flow<PlaylistEntity?> = playlistDao.getPlaylistById(id)

    suspend fun updatePlaylist(playlist: PlaylistEntity) = playlistDao.updatePlaylist(playlist)

    suspend fun deletePlaylist(playlist: PlaylistEntity) = playlistDao.deletePlaylist(playlist)

    suspend fun addMediaToPlaylists(items: List<PlaylistMediaItem>) = playlistDao.addMediaToPlaylists(items)

    fun getAllMediaInPlaylist(playlistId: Int): Flow<List<MediaEntity>> = playlistDao.getMediaInPlaylist(playlistId)

    suspend fun getMediaNotInPlaylist(playlistId: Int): List<MediaEntity> = playlistDao.getMediaNotInPlaylist(playlistId)

    fun getPlaylistItemCount(playlistId: Int): Flow<Int> = playlistDao.getPlaylistItemCount(playlistId)

    suspend fun updateMediaPosition(item: PlaylistMediaItem) = playlistDao.updateMediaPosition(item)

    suspend fun removeMediaFromPlaylist(mediaIds: List<Int>, playlistId: Int) = playlistDao.removeMediaFromPlaylist(mediaIds, playlistId)

    suspend fun getMaxPositionInPlaylist(playlistId: Int): Int? = playlistDao.getMaxPositionInPlaylist(playlistId)
}
