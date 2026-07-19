package com.example.offlineplayer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.toMediaItem
import com.example.offlineplayer.data.repository.PlaylistRepository
import com.example.offlineplayer.data.repository.SettingsRepository
import com.example.offlineplayer.player.MediaControllerManager
import com.example.offlineplayer.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val controllerManager: MediaControllerManager,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository
): BaseViewModel() {

    private var playbackJob: Job? = null

    //Expose states from the manager
    val currentMediaItem = controllerManager.currentMediaItem
    val isPlaying = controllerManager.isPlaying
    val currentPosition = controllerManager.currentPosition
    val duration = controllerManager.duration
    val isShuffleModeEnabled = controllerManager.isShuffleModeEnabled
    val manualQueue = controllerManager.manualQueueState
    val upNextBase = controllerManager.upNextBaseState

    //Expose states from settings
    val keepScreenOn: StateFlow<Boolean> = settingsRepository.keepScreenOnFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.INITIAL_KEEP_SCREEN_ON
    )

    init {
        // Ensure the controller is connected when the app starts or reopens
        controllerManager.setupController()

        //Watch the isPlaying state to toggle ticker
        viewModelScope.launch {
            isPlaying.collect { playing ->
                if (playing) startPlaybackTicker()
                else stopPlaybackTicker()
            }
        }
    }

    //Player actions
    fun togglePlayPause() = controllerManager.togglePlayPause()
    fun seekToNext() = controllerManager.seekToNext()
    fun seekToPrevious() = controllerManager.seekToPrevious()
    fun seekTo(positionMs: Long) = controllerManager.seekTo(positionMs)
    fun toggleShuffle() = controllerManager.toggleShuffle()
    fun onRepeatModeClicked() {
        //TODO: Implement
        Log.d("OfflineAudioSuite", "MainVM: User requesting to change repeat mode")
    }

    fun playPlaylist(playlistId: Int) { //TODO: Change to controllerManager and not playlistRepository
        viewModelScope.launch {
            val isShuffleEnabled = settingsRepository.defaultShuffleFlow.first()
            playlistRepository.playPlaylistById(playlistId, startShuffled = isShuffleEnabled)
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
    fun moveBasePlaylistItem(fromIndex: Int, toIndex: Int) = controllerManager.moveBasePlaylistItem(fromIndex, toIndex)

    fun manualQueueSkipToIndex(index: Int) = controllerManager.manualQueueSkipToIndex(index)
    fun baseSkipToIndex(index: Int) = controllerManager.baseSkipToIndex(index)

    fun onAddToPlaylistClicked(id: Int) {
        viewModelScope.launch {
            //TODO: Open playlist picker
            Log.d("OfflineAudioSuite", "MainVM: User requesting to open playlist picker to add item $id to a playlist")
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
