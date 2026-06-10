package com.example.offlineplayer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.repository.SettingsRepository
import com.example.offlineplayer.util.MediaSortOrder
import com.example.offlineplayer.util.PlaylistsSortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUIState(
    val keepScreenOn: Boolean = SettingsRepository.INITIAL_KEEP_SCREEN_ON,
    val defaultMediaSortOrder: MediaSortOrder = SettingsRepository.INITIAL_MEDIA_SORT_ORDER,
    val defaultPlaylistsSortOrder: PlaylistsSortOrder = SettingsRepository.INITIAL_PLAYLISTS_SORT_ORDER,
    val defaultShuffle: Boolean = SettingsRepository.INITIAL_SHUFFLE
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
): ViewModel() {
    //Combine all flows into a single source for the UI
    val uiState: StateFlow<SettingsUIState> = combine(
        settingsRepository.keepScreenOnFlow,
        settingsRepository.defaultMediaSortOrderFlow,
        settingsRepository.defaultPlaylistsSortOrderFlow,
        settingsRepository.defaultShuffleFlow
    ) { screenOn, mediaSort, playlistsSort, shuffle ->
        SettingsUIState(
            keepScreenOn = screenOn,
            defaultMediaSortOrder = mediaSort,
            defaultPlaylistsSortOrder = playlistsSort,
            defaultShuffle = shuffle
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUIState() //Start with universal defaults
    )


    //Explicit auto-save functions
    fun setKeepScreenOn(newSetting: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(newSetting)
        }
    }

    fun setDefaultMediaSortOrder(newSortOrder: MediaSortOrder) {
        viewModelScope.launch {
            settingsRepository.setDefaultMediaSortOrder(newSortOrder)
        }
    }

    fun setDefaultPlaylistsSortOrder(newSortOrder: PlaylistsSortOrder) {
        viewModelScope.launch {
            settingsRepository.setDefaultPlaylistsSortOrder(newSortOrder)
        }
    }

    fun setDefaultShuffle(newSetting: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDefaultShuffle(newSetting)
        }
    }
}