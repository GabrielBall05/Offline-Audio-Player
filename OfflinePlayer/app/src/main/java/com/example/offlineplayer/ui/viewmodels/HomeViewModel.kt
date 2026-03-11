package com.example.offlineplayer.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.MediaDao
import com.example.offlineplayer.util.getMediaMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaDao: MediaDao,
    @ApplicationContext private val context: Context
) : ViewModel() {
    //Get all media entities from DB as StateFlow for UI to observe
    val allMedia = mediaDao.getAllMedia().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), //MAY CAUSE PROBLEMS WITH BACKGROUND AUDIO PLAYING
        initialValue = emptyList()
    )

    fun importMedia(uriList: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val entities = uriList.mapNotNull { uri ->
                try {
                    //Ensures persistent permission
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    //Extract metadata - Default MediaEntity is returned if extraction fails
                    //If only specific individual metadata fields are empty, default values are placed
                    getMediaMetadata(context, uri)
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Failed to get permission for $uri", e)
                    null //Skip this one
                }
            }
            mediaDao.insertMediaList(entities)
        }
    }
}