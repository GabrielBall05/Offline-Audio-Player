package com.example.offlineplayer.data

import androidx.room.Entity

@Entity(
    tableName = "playlist_media_items",
    primaryKeys = ["playlistId", "mediaId"] //Composite primary key
)
data class PlaylistMediaItems(
    val playlistId: Int,        //Id for playlist
    val mediaId: Int,           //Id for media
    val positionInPlaylist: Int //For custom ordering within playlist
) {


}