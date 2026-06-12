package com.example.offlineplayer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.PlaylistEntity
import com.example.offlineplayer.data.repository.PlaylistRepository
import com.example.offlineplayer.data.repository.SettingsRepository
import com.example.offlineplayer.util.PlaylistsSortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel() {

    //For searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    //Sort State
    private val _sortOrder = MutableStateFlow(SettingsRepository.INITIAL_PLAYLISTS_SORT_ORDER)
    val sortOrder = _sortOrder.asStateFlow()

    //Get all playlist entities from the db
    private val _allPlaylists = playlistRepository.allPlaylists

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
            PlaylistsSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            PlaylistsSortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            PlaylistsSortOrder.DATE_CREATED_MOST_RECENT -> filtered.sortedByDescending { it.dateCreated }
            PlaylistsSortOrder.DATE_CREATED_LEAST_RECENT -> filtered.sortedBy { it.dateCreated }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            _sortOrder.value = settingsRepository.defaultPlaylistsSortOrderFlow.first()
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSortOrderChange(newOrder: PlaylistsSortOrder) {
        _sortOrder.value = newOrder
    }

    fun createPlaylist(playlist: PlaylistEntity) = launchWithoutLoading {
        playlistRepository.insertPlaylist(playlist) //Perform db insert
    }

    fun editPlaylist(playlist: PlaylistEntity) = launchWithoutLoading {
        playlistRepository.updatePlaylist(playlist) //Perform db insert
    }

    fun deletePlaylist(playlist: PlaylistEntity) = launchWithLoading {
        playlistRepository.deletePlaylist(playlist)
    }

    fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) = launchWithLoading {
        playlistRepository.addMediaToPlaylists(mediaIds, playlistIds)
    }

    suspend fun getMediaNotInPlaylist(playlistId: Int): List<MediaEntity> {
        return playlistRepository.getMediaNotInPlaylist(playlistId)
    }
}