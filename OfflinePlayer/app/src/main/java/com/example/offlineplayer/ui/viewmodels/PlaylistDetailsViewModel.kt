package com.example.offlineplayer.ui.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.domain.MediaInteractor
import com.example.offlineplayer.data.domain.PlaylistInteractor
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.PlaylistEntity
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val mediaInteractor: MediaInteractor,
    private val playlistInteractor: PlaylistInteractor,
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    //Get clicked playlist id straight from navigation arguments
    private val playlistId: Int = checkNotNull(savedStateHandle["id"])

    //Get the actual playlist from db
    val playlist = playlistInteractor.getPlaylistById(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    //Get the number of media items in this playlist
    val itemCount = playlistInteractor.getPlaylistItemCount(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    //Get all media items in this playlist
    val playlistMedia = playlistInteractor.getMediaInPlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //For searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    var filteredMedia = combine(playlistMedia, _searchQuery) { media, query ->
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

    //Available playlists for PlaylistPicker
    private val _availablePlaylists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val availablePlaylists = _availablePlaylists.asStateFlow()

    fun refreshAvailablePlaylists(mediaIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlists = playlistInteractor.getPlaylistsNotHavingMediaList(mediaIds)
            _availablePlaylists.value = playlists
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        _selectedMediaIds.value = emptySet()
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

    fun getCommonCreator(ids: List<Int>): String {
        return filteredMedia.value.filter { it.mediaId in ids }.getCommonCreator()
    }

    fun getCommonArtwork(ids: List<Int>): String? {
        return filteredMedia.value.filter { it.mediaId in ids }.getCommonArtwork()
    }

    fun editPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistInteractor.editPlaylist(playlist)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistInteractor.deletePlaylist(playlist)
        }
    }

    fun removeMediaFromPlaylist(ids: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistInteractor.removeMediaFromPlaylist(ids, playlistId)
        }
    }

    fun updateMediaItem(item: MediaEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaInteractor.updateMedia(item) //Perform db update
        }
    }

    fun moveMediaItemPosition(fromMediaId: Int, toMediaId: Int) {
        //Get indices from full media list
        val fromPos = playlistMedia.value.indexOfFirst { it.mediaId == fromMediaId }
        val toPos = playlistMedia.value.indexOfFirst { it.mediaId == toMediaId }
        if (fromPos == -1 || toPos == -1) return

        //Perform db update
        viewModelScope.launch(Dispatchers.IO) {
            playlistInteractor.moveMediaItemPositionInPlaylist(playlistId, fromMediaId, toMediaId, fromPos, toPos)
        }
    }

    fun updateCreatorBulk(creator: String, ids: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaInteractor.updateCreatorBulk(creator, ids)
        }
    }

    fun updateArtworkBulk(artworkUri: String?, ids: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaInteractor.updateArtworkBulk(artworkUri, ids) //Perform db update
        }
    }

    fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistInteractor.addMediaToPlaylists(mediaIds, playlistIds)
        }
    }

    suspend fun getMediaNotInPlaylist(): List<MediaEntity> {
        return playlistInteractor.getMediaNotInPlaylist(playlistId)
    }
}
