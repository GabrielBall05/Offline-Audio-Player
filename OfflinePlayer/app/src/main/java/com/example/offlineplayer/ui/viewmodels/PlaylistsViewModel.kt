package com.example.offlineplayer.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.PlaylistDao
import com.example.offlineplayer.data.PlaylistEntity
import com.example.offlineplayer.player.MediaControllerManager
import com.example.offlineplayer.util.PlaylistSortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val controllerManager: MediaControllerManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    //For searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    //Sort State
    private val _sortOrder = MutableStateFlow(PlaylistSortOrder.NAME_ASC)
    val sortOrder = _sortOrder.asStateFlow()

    //Get all playlist entities from the db
    private val _allPlaylists = playlistDao.getAllPlaylists()

    //Filter full list by combining with search query
    val filteredPlaylists = combine(_allPlaylists, _searchQuery, _sortOrder) { playlists, query, sort ->
        //Filter first
        val filtered = if (query.isBlank()) //Search field empty, show whole list
            playlists
        else { //Only show list where title or description (if exists) contains query (case insensitive)
            playlists.filter { item ->
                item.name.contains(query, ignoreCase = true) ||
                (item.description?.contains(query, ignoreCase = true) ?: false)
            }
        }
        //Then sort
        when (sort) {
            PlaylistSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            PlaylistSortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            PlaylistSortOrder.ITEM_COUNT_ASC -> filtered //TODO: Implement sort by item count once item count is implemented
            PlaylistSortOrder.ITEM_COUNT_DESC -> filtered //TODO: Implement sort by item count once item count is implemented
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
            playlistDao.insertPlaylist(playlist)
        }
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