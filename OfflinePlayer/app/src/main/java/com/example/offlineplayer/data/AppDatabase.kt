package com.example.offlineplayer.data

import androidx.room.Database
import androidx.room.RoomDatabase

//All entities live here (Media, Playlists, etc)
@Database(
    entities = [
        MediaEntity::class,
        PlaylistEntity::class,
        PlaylistMediaItems::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    //Connect Database to Dao
    abstract fun mediaDao(): MediaDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun settingsDao(): SettingsDao
}