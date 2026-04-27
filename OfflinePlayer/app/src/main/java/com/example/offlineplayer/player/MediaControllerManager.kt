package com.example.offlineplayer.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.offlineplayer.data.MediaEntity
import com.example.offlineplayer.data.toMediaItem
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaControllerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller: MediaController? = null
        private set


    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem = _currentMediaItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()


    init {
        setupController()
    }

    //TODO: Make work with shuffle
    fun playNow(mediaEntity: MediaEntity) {
        controller?.let { player ->
            val mediaItem = mediaEntity.toMediaItem()

            //If something is already playing, insert at next spot and go to it. Otherwise, set it and play
            if (player.mediaItemCount > 0) {
                val nextIndex = player.currentMediaItemIndex + 1
                player.addMediaItem(nextIndex, mediaItem)
                player.seekToNextMediaItem()
            } else {
                player.setMediaItem(mediaItem)
                player.prepare()
            }

            //Ensure its playing (unpause if needed)
            player.play()
        }
    }

    //TODO: Make work with shuffle
    fun addToQueue(mediaEntity: MediaEntity) {
        controller?.let { player ->
            val mediaItem = mediaEntity.toMediaItem()

            //If something is already playing, insert at next spot. Otherwise, fresh start
            if (player.mediaItemCount > 0) {
                val nextIndex = player.currentMediaItemIndex + 1
                player.addMediaItem(nextIndex, mediaItem)
            } else {
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        }
    }

    fun playPlaylist(mediaList: List<MediaEntity>, startIndex: Int = 0) {
        if (mediaList.isEmpty()) {
            Log.d("OfflineAudioSuite", "MediaControllerManager: Cannot play empty playlist")
            return
        }
        controller?.let { player ->
            val mediaItems = mediaList.map { it.toMediaItem() }
            //Replace current queue with the new playlist
            player.setMediaItems(mediaItems, startIndex, 0)
            player.prepare()
            player.play()
        }
    }

    fun play() = controller?.play()

    fun pause() = controller?.pause()

    fun seekToNext() {
        controller?.let {
            it.seekToNext()
            it.play() //Force play
        }
    }

    fun seekToPrevious() {
        controller?.let {
            it.seekToPrevious()
            it.play() //Force play
        }
    }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    fun stop() { controller?.stop() }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying()) it.pause() else it.play()
        }
    }


    private fun setupController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            val player = controllerFuture?.get()
            controller = player

            //Attach listener to track state changes
            player?.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    _currentMediaItem.value = mediaItem
                    _duration.value = player.duration.coerceAtLeast(0L)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    _isPlaying.value = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = player.duration.coerceAtLeast(0L)
                    }
                }
            })
        }, MoreExecutors.directExecutor())
    }

    fun updateCurrentPosition() {
        controller?.let {
            _currentPosition.value = it.currentPosition
            _duration.value = it.duration.coerceAtLeast(0L)
        }
    }

    fun releaseController() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}