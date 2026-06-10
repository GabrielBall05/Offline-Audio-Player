package com.example.offlineplayer.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.PlaylistEntity
import com.example.offlineplayer.data.repository.MediaRepository
import com.example.offlineplayer.data.repository.PlaylistRepository
import com.example.offlineplayer.data.repository.SettingsRepository
import com.example.offlineplayer.util.MediaSortOrder
import com.example.offlineplayer.util.getCommonArtwork
import com.example.offlineplayer.util.getCommonCreator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    settingsRepository: SettingsRepository
): ViewModel() {

    //For searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    //Sort State
    private val _sortOrder = MutableStateFlow(SettingsRepository.INITIAL_MEDIA_SORT_ORDER)
    val sortOrder = _sortOrder.asStateFlow()

    //Get all media entities from DB using the interactor's shared flow
    private val _allMedia = mediaRepository.allMedia

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
    private val _selectedMediaIds = MutableStateFlow<List<Int>>(emptyList())
    val selectedMediaIds = _selectedMediaIds.asStateFlow()
    val isAnySelected = selectedMediaIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isAllSelected = filteredMedia.combine(selectedMediaIds) { all, selected ->
        all.isNotEmpty() && all.size == selected.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    //Available playlists for PlaylistPicker
    private val _availablePlaylists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val availablePlaylists = _availablePlaylists.asStateFlow()

    val hasMedia: StateFlow<Boolean> = _allMedia
        .map { list -> list.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _sortOrder.value = settingsRepository.defaultMediaSortOrderFlow.first()
        }
    }

    fun refreshAvailablePlaylists(mediaIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlists = playlistRepository.getPlaylistsNotHavingMediaList(mediaIds)
            _availablePlaylists.value = playlists
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        _selectedMediaIds.value = emptyList() //Clear selections
    }

    fun onSortOrderChange(newOrder: MediaSortOrder) {
        _sortOrder.value = newOrder
    }

    fun toggleSelection(mediaId: Int) {
        _selectedMediaIds.update { currentList ->
            if (currentList.contains(mediaId)) currentList - mediaId  //Remove from selection list
            else currentList + mediaId  //Add to selection list
        }
    }

    fun toggleSelectAll() {
        _selectedMediaIds.value =
            if (_selectedMediaIds.value.size == filteredMedia.value.size) emptyList() //Deselect all
            else filteredMedia.value.map { it.mediaId }.toList() //Select all
    }

    fun clearSelection() {
        _selectedMediaIds.value = emptyList()
    }

    fun getCommonCreator(ids: List<Int>): String {
        return filteredMedia.value.filter { it.mediaId in ids }.getCommonCreator()
    }

    fun getCommonArtwork(ids: List<Int>): String? {
        return filteredMedia.value.filter { it.mediaId in ids }.getCommonArtwork()
    }

    fun importMedia(uriList: List<Uri>) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mediaRepository.importMedia(uriList)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteMediaByIds(ids: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaRepository.deleteMediaList(ids) //Perform db deletions
            _selectedMediaIds.value -= ids.toSet() //Remove from selection list since they no longer exist
        }
    }

    fun updateMediaItem(item: MediaEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaRepository.updateMedia(item) //Perform db update
        }
    }

    fun updateCreatorBulk(creator: String, ids: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaRepository.updateCreatorBulk(creator, ids) //Perform db update
        }
    }

    fun updateArtworkBulk(artworkUri: String?, ids: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaRepository.updateArtworkBulk(artworkUri, ids) //Perform db update
        }
    }

    fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistRepository.addMediaToPlaylists(mediaIds, playlistIds) //Perform db insert
        }
    }

    fun createPlaylist(playlist: PlaylistEntity, mediaIdsContext: List<Int> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistRepository.insertPlaylist(playlist) //Perform db insert
            if (mediaIdsContext.isNotEmpty()) refreshAvailablePlaylists(mediaIdsContext)
        }
    }
}
