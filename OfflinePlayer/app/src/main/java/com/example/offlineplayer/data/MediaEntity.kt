package com.example.offlineplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    val mediaId: Int = 0,        //Auto-incremented and auto-generated primary key
    val uri: String,        //Location of the file
    val title: String,      //Title of the song, podcast, audiobook, etc
    val creator: String,    //Name of artist, podcaster, author, etc
    val duration: Long,     //Length in ms
    val fileName: String,   //Actual name of the file
    val dateAdded: Long,    //Date added (used for recently added section)
    val mimeType: String?,  //File type (ex: mp3, wav)
    val artworkUri: String? //Local path to associated image
) {


}