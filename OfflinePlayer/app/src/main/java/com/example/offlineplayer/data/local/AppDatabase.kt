package com.example.offlineplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MediaEntity::class,
        PlaylistEntity::class,
        PlaylistMediaItem::class,
        SettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    //Connect Database to Dao
    abstract fun mediaDao(): MediaDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "offline_player_db"
    }
}