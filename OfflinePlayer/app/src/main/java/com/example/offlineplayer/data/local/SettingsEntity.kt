package com.example.offlineplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = SettingsEntity.TABLE_NAME)
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,    //Always 0 to ensure only one row exists

) {
    companion object {
        const val TABLE_NAME = "app_settings"
    }
}