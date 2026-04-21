package com.example.offlineplayer.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.offlineplayer.player.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val controllerManager: MediaControllerManager
): ViewModel() {
    //Expose states from the manager
    val currentMediaItem = controllerManager.currentMediaItem
    val isPlaying = controllerManager.isPlaying
    val currentPosition = controllerManager.currentPosition
    val duration = controllerManager.duration

    //UI actions
    fun togglePlayPause() = controllerManager.togglePlayPause()
    fun seekToNext() = controllerManager.seekToNext()
    fun seekToPrevious() = controllerManager.seekToPrevious()

    //Clean up controller when app truly closes
    override fun onCleared() {
        super.onCleared()
        controllerManager.releaseController()
    }
}