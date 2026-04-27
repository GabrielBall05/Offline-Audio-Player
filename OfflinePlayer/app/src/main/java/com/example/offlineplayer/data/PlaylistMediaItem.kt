package com.example.offlineplayer.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "playlist_media_items",
    primaryKeys = ["playlistId", "mediaId"], //Composite primary key
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["mediaId"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistMediaItem(
    val playlistId: Int,        //Id for playlist
    val mediaId: Int,           //Id for media
    val positionInPlaylist: Int //For custom ordering within playlist
) {

}