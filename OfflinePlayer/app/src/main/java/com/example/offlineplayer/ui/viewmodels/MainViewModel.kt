package com.example.offlineplayer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.PlaylistEntity
import com.example.offlineplayer.data.local.toMediaItem
import com.example.offlineplayer.data.repository.MediaRepository
import com.example.offlineplayer.data.repository.PlaylistRepository
import com.example.offlineplayer.data.repository.SettingsRepository
import com.example.offlineplayer.player.MediaControllerManager
import com.example.offlineplayer.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val controllerManager: MediaControllerManager,
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository
): BaseViewModel() {
    private var playbackJob: Job? = null

    //Expose states from the manager
    val currentMediaItem = controllerManager.currentMediaItem
    val isPlaying = controllerManager.isPlaying
    val currentPosition = controllerManager.currentPosition
    val duration = controllerManager.duration
    val isRepeatingCurrent = controllerManager.repeatingCurrentState
    val isShuffleModeEnabled = controllerManager.isShuffling
    val manualQueue = controllerManager.manualQueueState
    val upNext = controllerManager.upNextState

    //Full db entry for the currently playing media item
    private val _currentMediaEntity = MutableStateFlow<MediaEntity?>(null)
    val currentMediaEntity = _currentMediaEntity.asStateFlow()

    //Available playlists for playlist picker
    private val _availablePlaylists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val availablePlaylists = _availablePlaylists.asStateFlow()

    //Expose states from settings
    val keepScreenOn: StateFlow<Boolean> = settingsRepository.keepScreenOnFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.INITIAL_KEEP_SCREEN_ON
    )

    init {
        //Ensure the controller is connected when the app starts or reopens
        controllerManager.setupController()

        //Watch the isPlaying state to toggle ticker
        viewModelScope.launch {
            isPlaying.collect { playing ->
                if (playing) startPlaybackTicker()
                else stopPlaybackTicker()
            }
        }

        viewModelScope.launch {
            currentMediaItem.collect { mediaItem ->
                val id = mediaItem?.mediaMetadata?.extras?.getString("ORIGINAL_MEDIA_ID")?.toIntOrNull()
                    ?: mediaItem?.mediaId?.toIntOrNull()

                if (id != null) {
                    val entity = withContext(Dispatchers.IO) {
                        mediaRepository.getMediaById(id)
                    }
                    _currentMediaEntity.value = entity
                }
                else _currentMediaEntity.value = null
            }
        }
    }

    //Player actions
    fun togglePlayPause() = controllerManager.togglePlayPause()
    fun seekToNext() = controllerManager.seekToNext()
    fun seekToPrevious() = controllerManager.seekToPrevious()
    fun seekTo(positionMs: Long) = controllerManager.seekTo(positionMs)
    fun toggleShuffle() = controllerManager.toggleShuffle()
    fun toggleRepeatMode() = controllerManager.toggleRepeatMode()

    fun playPlaylist(playlistId: Int) { //TODO: Change to controllerManager and not playlistRepository
        viewModelScope.launch {
            //val isShuffleEnabled = settingsRepository.defaultShuffleFlow.first()
            playlistRepository.playPlaylistById(playlistId, false) //TODO: Fix with the saved shuffle on/off once ready
        }
    }

    fun addPlaylistToQueue(playlistId: Int) { //TODO: Change to controllerManager and not playlistRepository
        viewModelScope.launch {
            playlistRepository.addPlaylistToQueue(playlistId)
            sendUiEvent(UiEvent.ShowToast("Added playlist to queue"))
        }
    }

    fun addMediaToQueue(mediaList: List<MediaEntity>) {
        controllerManager.addToQueue(mediaList.map { it.toMediaItem() })
        sendUiEvent(UiEvent.ShowToast("Added ${mediaList.size} item${if (mediaList.size > 1) "s" else ""} to queue"))
    }

    fun playMediaNow(media: MediaEntity) = controllerManager.playNow(media.toMediaItem())

    fun clearQueue() = controllerManager.clearQueue()

    fun moveManualQueueItem(fromIndex: Int, toIndex: Int) = controllerManager.moveManualQueueItem(fromIndex, toIndex)
    fun moveUpNextItem(fromIndex: Int, toIndex: Int) = controllerManager.moveUpNextItem(fromIndex, toIndex)

    fun manualQueueSkipToIndex(index: Int) = controllerManager.manualQueueSkipToIndex(index)
    fun upNextSkipToIndex(index: Int) = controllerManager.upNextSkipToIndex(index)

    fun manualQueueRemoveItemAtIndex(index: Int) = controllerManager.manualQueueRemoveItemAtIndex(index)
    fun upNextRemoveItemAtIndex(index: Int) = controllerManager.upNextRemoveItemAtIndex(index)

    fun createPlaylist(playlist: PlaylistEntity, mediaIdContext: Int) = viewModelScope.launch {
        playlistRepository.insertPlaylist(playlist)
        refreshAvailablePlaylists(mediaIdContext)
        sendUiEvent(UiEvent.ShowToast("Playlist created"))
    }

    fun addMediaToPlaylists(mediaId: Int, playlistIds: List<Int>) = viewModelScope.launch {
        playlistRepository.addMediaToPlaylists(listOf(mediaId), playlistIds)
        sendUiEvent(UiEvent.ShowToast("Added to ${playlistIds.size} playlist${if (playlistIds.size > 1) "s" else ""}"))
    }

    fun refreshAvailablePlaylists(mediaId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlists = playlistRepository.getPlaylistsNotHavingMediaList(listOf(mediaId))
            _availablePlaylists.value = playlists
        }
    }

    private fun startPlaybackTicker() {
        playbackJob?.cancel() //Clear any existing job
        playbackJob = viewModelScope.launch {
            while (true) {
                controllerManager.updateCurrentPosition()
                delay(500L) //Tick every 500ms
            }
        }
    }

    private fun stopPlaybackTicker() {
        playbackJob?.cancel()
        playbackJob = null
    }

    //Clean up controller when app truly closes
    override fun onCleared() {
        super.onCleared()
        controllerManager.releaseController()
    }
}
