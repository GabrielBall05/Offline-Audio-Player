package com.example.offlineplayer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    //CREATE playlist
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    //READ - Get playlist by id
    @Query("SELECT * FROM playlists WHERE playlistId = :id")
    fun getPlaylistById(id: Int): Flow<PlaylistEntity?>

    //READ - Get all playlists
    @Query("SELECT * FROM playlists ORDER BY dateCreated DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    //UPDATE playlist
    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    //DELETE playlist
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    //======================== PLAYLIST MEDIA ITEMS ========================//

    //CREATE - Add song to playlist
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMediaToPlaylist(items: List<PlaylistMediaItem>)

    //READ - Get all songs in playlist
    @Query("""
        SELECT MI.* FROM media_items AS MI
        INNER JOIN playlist_media_items AS PMI
            ON MI.mediaId = PMI.mediaId
        WHERE PMI.playlistId = :playlistId
        ORDER BY PMI.positionInPlaylist ASC
    """)
    fun getMediaInPlaylist(playlistId: Int): Flow<List<MediaEntity>>

    //UPDATE - Change position in playlist
    @Update
    suspend fun updateMediaPosition(item: PlaylistMediaItem)

    //DELETE - Remove media from playlist
    @Query("""
        DELETE FROM playlist_media_items
        WHERE mediaId IN (:mediaIds)
        AND playlistId = :playlistId
    """)
    suspend fun removeMediaFromPlaylist(mediaIds: List<Int>, playlistId: Int)

    //Get the max position in a given playlist for ordering new media items
    @Query("""
        SELECT MAX(positionInPlaylist)
        FROM playlist_media_items
        WHERE playlistId = :playlistId
    """)
    suspend fun getMaxPositionInPlaylist(playlistId: Int): Int?
}