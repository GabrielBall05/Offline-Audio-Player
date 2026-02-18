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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMediaToPlaylist(item: PlaylistMediaItems)

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
    suspend fun updateMediaPosition(item: PlaylistMediaItems)

    //DELETE - Remove song from playlist
    suspend fun removeMediaFromPlaylist(item: PlaylistMediaItems)
}