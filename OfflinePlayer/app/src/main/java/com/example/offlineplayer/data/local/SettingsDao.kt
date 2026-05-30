package com.example.offlineplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    //CREATE settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)

    @Query("SELECT * FROM ${SettingsEntity.TABLE_NAME} WHERE id = 0")
    fun getSettings(): Flow<SettingsEntity?>
}