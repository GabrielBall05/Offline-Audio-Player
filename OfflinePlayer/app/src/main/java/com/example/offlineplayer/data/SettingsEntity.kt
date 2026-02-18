package com.example.offlineplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,            //Always 0 to ensure only one row exists
    val isDarkMode: Boolean = true,         //Dark mode - default on
    val crossfadeSeconds: Int = 0,          //Seconds for crossfading media items
    val useDynamicColors: Boolean = true    //For Android "Material You" feature
) {


}