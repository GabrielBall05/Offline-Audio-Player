package com.example.offlineplayer.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.data.local.toMediaItem
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

    //Current item for UI
    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem = _currentMediaItem.asStateFlow()

    //Playing state for UI
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    //Current position state for UI
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    //Duration state for UI
    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    //Shuffle state for UI
    private val _isShuffleModeEnabled = MutableStateFlow(false)
    val isShuffleModeEnabled = _isShuffleModeEnabled.asStateFlow()

    //Source of truth - Original playlist in order
    private var sourcePlaylist: List<MediaItem> = emptyList()

    //Stable shuffled playlist
    private var shuffledPlaylist: List<MediaItem> = emptyList()

    //User's manual FIFO queue
    private val manualQueue = ArrayDeque<MediaItem>()

    //To give ExoPlayer
    private var activeTimeline: List<MediaItem> = emptyList()

    //Tracks position in base playlist
    private var lastKnownBaseItem: MediaItem? = null


    init {
        setupController()
    }

    fun playNow(mediaItem: MediaItem) {
        controller?.let { player ->
            //If something is already playing, insert at next spot and go to it. Otherwise, set it and play
            if (player.mediaItemCount > 0) {
                val nextIndex = player.currentMediaItemIndex + 1
                player.addMediaItem(nextIndex, mediaItem)
                //player.seekToNextMediaItem()
                player.seekToNext()
            } else {
                player.setMediaItem(mediaItem)
                player.prepare()
            }
            player.play() //Ensure its playing (unpause if needed)
        }
    }

    fun playPlaylist(mediaItems: List<MediaItem>, startItemIndex: Int = 0) {
        sourcePlaylist = mediaItems
        manualQueue.clear()

        val startingItem = sourcePlaylist.getOrNull(startItemIndex)
        lastKnownBaseItem = startingItem

        val remaining = sourcePlaylist.filterIndexed { index, _ -> index != startItemIndex }.shuffled()
        shuffledPlaylist = listOfNotNull(startingItem) + remaining

        rebuildTimeline(startingItem, isStartingNew = true)
    }

    fun addToQueue(mediaItem: MediaItem) {
        manualQueue.addLast(mediaItem)
        rebuildTimeline(controller?.currentMediaItem, isStartingNew = false)
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
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun toggleShuffle() {
        _isShuffleModeEnabled.value = !_isShuffleModeEnabled.value
        val current = controller?.currentMediaItem

        if (_isShuffleModeEnabled.value) {
            val anchorItem = lastKnownBaseItem ?: current ?: sourcePlaylist.firstOrNull()
            val remaining = sourcePlaylist.filter { it.mediaId != anchorItem?.mediaId }.shuffled()
            shuffledPlaylist = listOfNotNull(anchorItem) + remaining
        }

        rebuildTimeline(current, isStartingNew = false)
    }

    fun updateCurrentPosition() {
        controller?.let {
            _currentPosition.value = it.currentPosition
            _duration.value = it.duration.coerceAtLeast(0L)
        }
    }

    private fun rebuildTimeline(currentPlayingItem: MediaItem?, isStartingNew: Boolean) {
        val player = controller ?: return
        if (sourcePlaylist.isEmpty()) return

        val current = currentPlayingItem ?: sourcePlaylist.first()

        //Determine which timeline we're following
        val basePlaylist = if (_isShuffleModeEnabled.value) shuffledPlaylist else sourcePlaylist

        //Find where we are in the timeline
        var baseIndex = basePlaylist.indexOfFirst { it.mediaId == current.mediaId }

        if (baseIndex != -1) { //We are in base playlist - update tracker
            lastKnownBaseItem = current
        } else  { //Playing manual queue item - resume from last known base item
            baseIndex = basePlaylist.indexOfFirst { it.mediaId == lastKnownBaseItem?.mediaId }
        }

        //Get everything after current position
        val remainingBaseItems = if (baseIndex != -1 && baseIndex + 1 < basePlaylist.size) {
            basePlaylist.subList(baseIndex + 1, basePlaylist.size)
        } else {
            emptyList()
        }

        //Construct final active timeline
        activeTimeline = listOfNotNull(current) + manualQueue + remainingBaseItems

        //Give to ExoPlayer
        if (isStartingNew) { //Hard reset for new playlist
            player.setMediaItems(activeTimeline)
            player.prepare()
            player.play()
        } else { //Seamless update - replace upcoming tracks without stopping currently playing media item
            val nextIndex = player.currentMediaItemIndex + 1
            val itemsToAdd = manualQueue.toList() + remainingBaseItems

            //Clear everything after currently playing song
            if (player.mediaItemCount > nextIndex) {
                player.removeMediaItems(nextIndex, player.mediaItemCount)
            }

            //Inject newly calculated future
            player.addMediaItems(nextIndex, itemsToAdd)
        }
    }

    fun setupController() {
        if (controllerFuture != null) return

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                val player = controllerFuture?.get() ?: return@addListener
                controller = player

                // Sync initial state from the connected session
                _currentMediaItem.value = player.currentMediaItem
                _isPlaying.value = player.isPlaying
                _duration.value = player.duration.coerceAtLeast(0L)
                _currentPosition.value = player.currentPosition

                //Attach listener to track state changes
                player.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        _currentMediaItem.value = mediaItem
                        _duration.value = player.duration.coerceAtLeast(0L)

                        //Check if the media item we just transitioned to was the next item in the manual queue
                        if (manualQueue.isNotEmpty() && mediaItem?.mediaId == manualQueue.first().mediaId) {
                            manualQueue.removeFirst() //Pop
                        } else { //Update tracker
                            lastKnownBaseItem = mediaItem
                        }
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
            } catch (e: Exception) {
                Log.e("OfflineAudioSuite", "MediaControllerManager: Failed to connect to MediaController", e)
                controllerFuture = null
            }
        }, MoreExecutors.directExecutor())
    }

    fun releaseController() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
            controllerFuture = null
            controller = null
        }
    }
}