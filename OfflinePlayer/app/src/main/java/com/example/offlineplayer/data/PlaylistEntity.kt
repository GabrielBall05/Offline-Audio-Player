package com.example.offlineplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val playlistId: Int = 0,    //Auto-incremented and auto-generated primary key
    val name: String,           //Name of playlist
    val description: String?,   //Optional description
    val dateCreated: Long,      //Date added
    val coverImage: String?      //Local path to associated image
) {


}