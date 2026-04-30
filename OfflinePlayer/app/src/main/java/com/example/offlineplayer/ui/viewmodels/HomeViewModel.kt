package com.example.offlineplayer.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.domain.MediaInteractor
import com.example.offlineplayer.data.domain.PlaylistInteractor
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.util.MediaSortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaInteractor: MediaInteractor,
    private val playlistInteractor: PlaylistInteractor,
    @param:ApplicationContext private val context: Context
): ViewModel() {

    //For searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    //Sort State
    private val _sortOrder = MutableStateFlow(MediaSortOrder.TITLE_ASC)
    val sortOrder = _sortOrder.asStateFlow()

    //Get all media entities from DB using the interactor's shared flow
    private val _allMedia = mediaInteractor.allMedia

    //Filter full list by combining with the search query (this is the list shown in UI)
    val filteredMedia = combine(_allMedia, _searchQuery, _sortOrder) { media, query, sort ->
        //Filter first
        val filtered = if (query.isBlank()) { //Search field empty, show whole list
            media
        } else { //Only show list where title or creator contains the search query (case insensitive)
            media.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                item.creator.contains(query, ignoreCase = true)
            }
        }
        //Then sort
        when (sort) {
            MediaSortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            MediaSortOrder.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            MediaSortOrder.CREATOR_ASC -> filtered.sortedBy { it.creator.lowercase() }
            MediaSortOrder.CREATOR_DESC -> filtered.sortedByDescending { it.creator.lowercase() }
            MediaSortOrder.DURATION_ASC -> filtered.sortedBy { it.duration }
            MediaSortOrder.DURATION_DESC -> filtered.sortedByDescending { it.duration }
            MediaSortOrder.DATE_ADDED_MOST_RECENT -> filtered.sortedByDescending { it.dateAdded }
            MediaSortOrder.DATE_ADDED_LEAST_RECENT -> filtered.sortedBy { it.dateAdded }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    //Selection variables
    private val _selectedMediaIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedMediaIds = _selectedMediaIds.asStateFlow()
    val isAnySelected = selectedMediaIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isAllSelected = filteredMedia.combine(selectedMediaIds) { all, selected ->
        all.isNotEmpty() && all.size == selected.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    //All playlists for PlaylistPicker
    val allPlaylists = playlistInteractor.allPlaylists
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        _selectedMediaIds.value = emptySet() //Clear selections
    }

    fun onSortOrderChange(newOrder: MediaSortOrder) {
        _sortOrder.value = newOrder
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

    fun importMedia(uriList: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaInteractor.importMedia(uriList)
        }
    }

    fun deleteMediaByIds(ids: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaInteractor.deleteMediaList(ids) //Perform db deletions
            _selectedMediaIds.value -= ids.toSet() //Remove from selection list since they no longer exist
        }
    }

    fun updateMediaItem(item: MediaEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaInteractor.updateMedia(item) //Perform db update
        }
    }

    fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistInteractor.addMediaToPlaylists(mediaIds, playlistIds)
        }
    }

    fun playMedia(media: MediaEntity) = mediaInteractor.playMedia(media)

    fun addMediaToQueue(media: MediaEntity) = mediaInteractor.addMediaToQueue(media)
}