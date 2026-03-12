package com.example.offlineplayer.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.MediaDao
import com.example.offlineplayer.data.MediaEntity
import com.example.offlineplayer.util.getMediaMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    //Search variables
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    //Selection variables
    private val _selectedMediaIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedMediaIds = _selectedMediaIds.asStateFlow()
    val isAnySelected = selectedMediaIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isAllSelected = allMedia.combine(selectedMediaIds) { all, selected ->
        all.isNotEmpty() && all.size == selected.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun toggleSelection(mediaId: Int) {
        val currentSet = _selectedMediaIds.value
        _selectedMediaIds.value =
            if (currentSet.contains(mediaId)) currentSet - mediaId //Remove from selection set
            else currentSet + mediaId //Add to selection set
    }

    fun toggleSelectAll() {
        _selectedMediaIds.value =
            if (_selectedMediaIds.value.size == allMedia.value.size) emptySet() //Deselect all
            else allMedia.value.map { it.mediaId }.toSet() //Select all
    }

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

    fun deleteMediaByIds(ids: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaDao.deleteMediaList(ids)
            _selectedMediaIds.value -= ids
        }
    }
}