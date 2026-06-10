package com.example.offlineplayer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.toMediaItem
import com.example.offlineplayer.data.repository.PlaylistRepository
import com.example.offlineplayer.player.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val controllerManager: MediaControllerManager,
    private val playlistRepository: PlaylistRepository
): ViewModel() {

    private var playbackJob: Job? = null

    //Expose states from the manager
    val currentMediaItem = controllerManager.currentMediaItem
    val isPlaying = controllerManager.isPlaying
    val currentPosition = controllerManager.currentPosition
    val duration = controllerManager.duration
    val isShuffleModeEnabled = controllerManager.isShuffleModeEnabled
    val manualQueue = controllerManager.manualQueueState
    val upNextBase = controllerManager.upNextBaseState

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
    fun playPlaylist(playlistId: Int) {
        viewModelScope.launch {
            playlistRepository.playPlaylistById(playlistId)
        }
    }
    fun addPlaylistToQueue(playlistId: Int) {
        viewModelScope.launch {
            playlistRepository.addPlaylistToQueue(playlistId)
        }
    }
    fun playMediaNow(media: MediaEntity) = controllerManager.playNow(media.toMediaItem())
    fun addMediaToQueue(mediaList: List<MediaEntity>) = controllerManager.addToQueue(mediaList.map { it.toMediaItem() })
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