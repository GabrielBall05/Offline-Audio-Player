package com.example.offlineplayer.data.local

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
    @Query("SELECT * FROM media_items ORDER BY title ASC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    //----------UPDATE----------
    //Update single media item
    @Update
    suspend fun updateMedia(media: MediaEntity)

    //Update creator bulk
    @Query("""
        UPDATE media_items
        SET creator = :creator
        WHERE mediaId IN (:ids)
    """)
    suspend fun updateCreatorBulk(creator: String, ids: List<Int>)

    //Update artwork bulk
    @Query("""
        UPDATE media_items
        SET artworkUri = :artworkUri
        WHERE mediaId IN (:ids)
    """)
    suspend fun updateArtworkBulk(artworkUri: String?, ids: List<Int>)


    //----------DELETE----------
    //Delete single media item
    @Delete
    suspend fun deleteMedia(media: MediaEntity)

    //Delete list of media items (by mediaId)
    @Query("DELETE FROM media_items WHERE mediaId IN (:mediaIds)")
    suspend fun deleteMediaList(mediaIds: List<Int>)
}