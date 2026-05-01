package com.example.offlineplayer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.domain.PlaylistInteractor
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.PlaylistEntity
import com.example.offlineplayer.util.PlaylistSortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistInteractor: PlaylistInteractor
) : ViewModel() {

    //For searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    //Sort State
    private val _sortOrder = MutableStateFlow(PlaylistSortOrder.NAME_ASC)
    val sortOrder = _sortOrder.asStateFlow()

    //Get all playlist entities from the db
    private val _allPlaylists = playlistInteractor.allPlaylists

    //Filter full list by combining with search query
    val filteredPlaylists = combine(_allPlaylists, _searchQuery, _sortOrder) { playlists, query, sort ->
        //Filter first
        val filtered = if (query.isBlank()) //Search field empty, show whole list
            playlists
        else { //Only show list where name or description (if exists) contains query (case insensitive)
            playlists.filter { item ->
                item.name.contains(query, ignoreCase = true) ||
                (item.description?.contains(query, ignoreCase = true) ?: false)
            }
        }
        //Then sort
        when (sort) {
            PlaylistSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            PlaylistSortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            PlaylistSortOrder.DATE_CREATED_MOST_RECENT -> filtered.sortedByDescending { it.dateCreated }
            PlaylistSortOrder.DATE_CREATED_LEAST_RECENT -> filtered.sortedBy { it.dateCreated }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSortOrderChange(newOrder: PlaylistSortOrder) {
        _sortOrder.value = newOrder
    }

    fun createPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistInteractor.createPlaylist(playlist)
        }
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

    fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistInteractor.addMediaToPlaylists(mediaIds, playlistIds)
        }
    }

    suspend fun getMediaNotInPlaylist(playlistId: Int): List<MediaEntity> {
        return playlistInteractor.getMediaNotInPlaylist(playlistId)
    }

    fun playPlaylistById(id: Int) {
        viewModelScope.launch {
            playlistInteractor.playPlaylistById(id)
        }
    }
}