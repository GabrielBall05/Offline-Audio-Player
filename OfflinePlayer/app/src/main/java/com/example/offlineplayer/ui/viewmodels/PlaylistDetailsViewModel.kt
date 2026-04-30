package com.example.offlineplayer.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.MediaDao
import com.example.offlineplayer.data.MediaEntity
import com.example.offlineplayer.data.PlaylistDao
import com.example.offlineplayer.data.PlaylistEntity
import com.example.offlineplayer.data.PlaylistMediaItem
import com.example.offlineplayer.player.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val mediaDao: MediaDao,
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

    fun editPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.updatePlaylist(playlist)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun removeMediaFromPlaylist(ids: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.removeMediaFromPlaylist(ids, playlistId)
        }
    }

    fun updateMediaItem(item: MediaEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaDao.updateMedia(item) //Perform db update
        }
    }

    fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            val allNewRefs = mutableListOf<PlaylistMediaItem>()

            //Loop through selected playlists
            playlistIds.forEach { pId ->
                //Get max position in current playlist - start at 0 if empty (returning null
                val currentMax = playlistDao.getMaxPositionInPlaylist(pId) ?: 0

                //Make a PlaylistMediaItem out of all selected media items and the current playlist
                val playlistRefs = mediaIds.mapIndexed { index, mId ->
                    PlaylistMediaItem(
                        playlistId = pId,
                        mediaId = mId,
                        positionInPlaylist = currentMax + index + 1 //Ensures proper incrementing
                    )
                }
                allNewRefs.addAll(playlistRefs)
            }

            //Insert all items into all playlists
            if (allNewRefs.isNotEmpty()) {
                playlistDao.addMediaToPlaylist(allNewRefs)
            }
        }
    }

    fun playMedia(media: MediaEntity) = controllerManager.playNow(media)

    fun addMediaToQueue(media: MediaEntity) = controllerManager.addToQueue(media)

    fun playPlaylistById(id: Int) {
        viewModelScope.launch {
            //Perform DB operation on IO thread
            val mediaList = withContext(Dispatchers.IO) {
                playlistDao.getMediaInPlaylist(id).first()
            }
            //MediaController methods must be called on the main thread
            controllerManager.playPlaylist(mediaList)
        }
    }
}