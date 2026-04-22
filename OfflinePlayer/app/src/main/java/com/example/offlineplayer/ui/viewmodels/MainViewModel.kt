package com.example.offlineplayer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineplayer.player.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val controllerManager: MediaControllerManager
): ViewModel() {

    private var playbackJob: Job? = null

    //Expose states from the manager
    val currentMediaItem = controllerManager.currentMediaItem
    val isPlaying = controllerManager.isPlaying
    val currentPosition = controllerManager.currentPosition
    val duration = controllerManager.duration

    //UI actions
    fun togglePlayPause() = controllerManager.togglePlayPause()
    fun seekToNext() = controllerManager.seekToNext()
    fun seekToPrevious() = controllerManager.seekToPrevious()
    fun seekTo(positionMs: Long) = controllerManager.seekTo(positionMs)

    init {
        //Watch the isPlaying state to toggle ticker
        viewModelScope.launch {
            isPlaying.collect { playing ->
                if (playing) startPlaybackTicker()
                else stopPlaybackTicker()
            }
        }
    }

    fun onAddToPlaylistClicked(id: Int) {
        viewModelScope.launch {
            //TODO: Open playlist picker
            Log.d("OfflineAudioSuite", "MainVM: User requesting to open playlist picker to add item $id to a playlist")
        }
    }

    fun onRepeatModeClicked() {
        //TODO: Implement
        Log.d("OfflineAudioSuite", "MainVM: User requesting to change repeat mode")
    }

    fun onPlayModeClicked() {
        //TODO: Implement
        Log.d("OfflineAudioSuite", "MainVM: User requesting to change play mode (shuffle/order)")
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