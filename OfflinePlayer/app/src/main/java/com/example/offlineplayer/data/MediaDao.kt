package com.example.offlineplayer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    //----------CREATE----------
    //Create single media item
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMedia(media: MediaEntity): Long

    //Create list of media items
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaList(mediaList: List<MediaEntity>): List<Long>

    //----------READ----------
    //Get all media items
    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    //----------UPDATE----------
    //Update single media item
    @Update
    suspend fun updateMedia(media: MediaEntity)

    //----------DELETE----------
    //Delete single media item
    @Delete
    suspend fun deleteMedia(media: MediaEntity)

    //Delete list of media items (by mediaId)
    @Query("DELETE FROM media_items WHERE mediaId IN (:idList)")
    suspend fun deleteMediaList(idList: List<Int>)
}