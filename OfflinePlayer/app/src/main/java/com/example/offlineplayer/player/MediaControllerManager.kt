package com.example.offlineplayer.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
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
        val player = controller ?: return
        val wasEmpty = player.mediaItemCount == 0

        //Insert media item at the from of manualQueue and rebuild the timeline
        manualQueue.addFirst(mediaItem)
        rebuildTimeline(player.currentMediaItem, isStartingNew = false)

        //If something was already playing, force a jump to the new item
        if (!wasEmpty) player.seekTo(player.currentMediaItemIndex + 1, 0L)

        //Force play
        player.play()
    }

    fun addToQueue(mediaItem: MediaItem) {
        //Insert media item to the end of manualQueue and rebuild the timeline
        manualQueue.addLast(mediaItem)
        rebuildTimeline(controller?.currentMediaItem, isStartingNew = false)
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
        //Return if there is nothing to play at all
        if (sourcePlaylist.isEmpty() && manualQueue.isEmpty()) return

        //Identify the active base playlist (can be empty)
        val basePlaylist = if (_isShuffleModeEnabled.value) shuffledPlaylist else sourcePlaylist

        //Find our position in the base playlist (if one exists)
        var baseIndex = -1
        if (basePlaylist.isNotEmpty() && currentPlayingItem != null) {
            baseIndex = basePlaylist.indexOfFirst { it.mediaId == currentPlayingItem.mediaId }
            if (baseIndex != -1) {
                lastKnownBaseItem = currentPlayingItem
            } else {
                baseIndex = basePlaylist.indexOfFirst { it.mediaId == lastKnownBaseItem?.mediaId }
            }
        }

        //Get everything after current position
        val remainingBaseItems = if (baseIndex != -1 && baseIndex + 1 < basePlaylist.size) {
            basePlaylist.subList(baseIndex + 1, basePlaylist.size)
        } else if (basePlaylist.isNotEmpty() && currentPlayingItem == null) {
            basePlaylist //Nothing is playing yet, so all base items are upcoming
        } else {
            emptyList() //No base playlist active, or we reached the end of it
        }

        //Construct the active timeline
        activeTimeline = listOfNotNull(currentPlayingItem) + manualQueue + remainingBaseItems
        //Return if there is still nothing to play
        if (activeTimeline.isEmpty()) return

        //Give to ExoPlayer
        if (isStartingNew || player.mediaItemCount == 0) { //Hard reset for new playlist or empty player
            player.setMediaItems(activeTimeline)
            player.prepare()
            player.play()
        } else {
            val nextIndex = player.currentMediaItemIndex + 1
            val itemsToAdd = manualQueue.toList() + remainingBaseItems

            if (player.mediaItemCount > nextIndex) {
                player.removeMediaItems(nextIndex, player.mediaItemCount)
            }
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

                        if (mediaItem == null) return
                        //Check if we just transitioned into the next manualQueue item - pop if so
                        if (manualQueue.isNotEmpty() && mediaItem.mediaId == manualQueue.first().mediaId) {
                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                                || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                                || reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                                manualQueue.removeFirst()
                            }
                        }
                        //Update tracker
                        val isInBasePlaylist = sourcePlaylist.any { it.mediaId == mediaItem.mediaId }
                        if (isInBasePlaylist) {
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

    fun stop() { controller?.stop() }

    fun releaseController() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
            controllerFuture = null
            controller = null
        }
    }
}