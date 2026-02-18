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
    //CREATE media item
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMedia(media: MediaEntity): Long

    //READ - Get all media items
    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    //UPDATE media item
    @Update
    suspend fun updateMedia(media: MediaEntity)

    //DELETE media item
    @Delete
    suspend fun deleteMedia(media: MediaEntity)
}