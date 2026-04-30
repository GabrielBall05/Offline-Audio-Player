package com.example.offlineplayer.data.local

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

fun MediaEntity.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(this.mediaId.toString())
        .setUri(this.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(this.title)
                .setArtist(this.creator)
                //TODO: Link artwork uri here
                .build()
        )
        .build()
}