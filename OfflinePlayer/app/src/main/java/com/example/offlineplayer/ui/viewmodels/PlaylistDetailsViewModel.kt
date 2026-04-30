package com.example.offlineplayer.ui.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.PlaylistDao
import com.example.offlineplayer.player.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val controllerManager: MediaControllerManager,
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    //Get clicked playlist id straight from navigation arguments
    private val playlistId: Int = checkNotNull(savedStateHandle["id"])

    //Get the actual playlist from db
    val playlist = playlistDao.getPlaylistById(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    //Get all media items in this playlist
    private val _playlistMedia = playlistDao.getMediaInPlaylist(playlistId)

    //For searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    var filteredMedia = combine(_playlistMedia, _searchQuery) { media, query ->
        if (query.isBlank()) //Search field empty, show whole list
            media
        else { //Only show list where title or creator contains query (case insensitive)
            media.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                item.creator.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //Selection variables
    private val _selectedMediaIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedMediaIds = _selectedMediaIds.asStateFlow()
    val isAnySelected = selectedMediaIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isAllSelected = filteredMedia.combine(selectedMediaIds) { all, selected ->
        all.isNotEmpty() && all.size == selected.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    //All playlists for PlaylistPicker
    val allPlaylists = playlistDao.getAllPlaylists()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun toggleSelection(mediaId: Int) {
        val currentSet = _selectedMediaIds.value
        _selectedMediaIds.update {
            if (currentSet.contains(mediaId)) currentSet - mediaId //Remove from selection set
            else currentSet + mediaId //Add to selection set
        }
    }

    fun toggleSelectAll() {
        _selectedMediaIds.value =
            if (_selectedMediaIds.value.size == filteredMedia.value.size) emptySet() //Deselect all
            else filteredMedia.value.map { it.mediaId }.toSet() //Select all
    }

    fun clearSelection() {
        _selectedMediaIds.value = emptySet()
    }
}