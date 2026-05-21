package com.example.offlineplayer.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.offlineplayer.data.local.MediaEntity
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

fun getMediaMetadata(context: Context, uri: Uri): MediaEntity {
    val retriever = MediaMetadataRetriever()
    val actualFileName = getFileNameFromUri(context, uri)

    return try {
        retriever.setDataSource(context, uri)

        //Attempt to retrieve specific metadata values
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: actualFileName.substringBeforeLast(".")
        val creator = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
            ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
            ?: "Unknown Creator"
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        val artworkUri = extractAlbumArt(context, retriever)

        Log.d("OfflineAudioSuite", "MetadataHelper: Returning MediaEntity with the extracted metadata values and other values:" +
            "\nuri: $uri" +
            "\ntitle: $title" +
            "\ncreator: $creator" +
            "\nduration: $duration" +
            "\nfileName: $actualFileName" +
            "\ndateAdded: ${System.currentTimeMillis()}" +
            "\nmimeType: $mimeType" +
            "\nartworkUri: $artworkUri")

        //Return MediaEntity with extracted metadata values (or defaults if it was null) and other info
        MediaEntity(
            mediaId = 0,
            uri = uri.toString(),
            title = title,
            creator = creator,
            duration = duration,
            fileName = actualFileName,
            dateAdded = System.currentTimeMillis(),
            mimeType = mimeType,
            artworkUri = artworkUri
        )
    } catch (e: Exception) {
        //If metadata is corrupted, return essentially blank default MediaEntity
        Log.e("OfflineAudioSuite", "MetadataHelper: Metadata extraction failed for $uri", e)
        MediaEntity(
            mediaId = 0,
            uri = uri.toString(),
            title = actualFileName.substringBeforeLast("."),
            creator = "Unknown Creator",
            duration = 0L,
            fileName = actualFileName,
            dateAdded = System.currentTimeMillis(),
            mimeType = null,
            artworkUri = null
        )
    } finally {
        retriever.release()
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String {
    var fileName = "Unknown File"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = it.getString(nameIndex)
            }
        }
    }
    return fileName
}

private fun extractAlbumArt(context: Context, retriever: MediaMetadataRetriever): String? {
    //Get embedded picture, return null if there isn't one to get
    val artBytes = retriever.embeddedPicture ?: return null

    //Create a unique filename for the image (UUID)
    val uniqueId = UUID.randomUUID().toString()
    val fileName = "art_$uniqueId.jpg"

    //Ensure directory exists, create it if not
    val directory = File(context.filesDir, "media_art").apply {
        if (!exists()) mkdirs()
    }

    val outputFile = File(directory, fileName)

    //Write bytes to file and return path
    return try {
        FileOutputStream(outputFile).use { it.write(artBytes) }
        outputFile.absolutePath
    } catch (e: Exception) {
        null
    }
}