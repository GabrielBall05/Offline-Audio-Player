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

    //Queue states for UI
    private val _manualQueueState = MutableStateFlow<List<MediaItem>>(emptyList())
    val manualQueueState = _manualQueueState.asStateFlow()
    private val _upNextBaseState = MutableStateFlow<List<MediaItem>>(emptyList())
    val upNextBaseState = _upNextBaseState.asStateFlow()

    //Source of truth - Original playlist in order
    private var sourcePlaylist: List<MediaItem> = emptyList()

    //Stable shuffled playlist
    private var shuffledPlaylist: List<MediaItem> = emptyList()

    //User's manual FIFO queue
    private val manualQueue = ArrayDeque<MediaItem>()

    //To give ExoPlayer
    private var activeTimeline: List<MediaItem> = emptyList()


    private var linearIndex = 0
    private var shuffledIndex = 0
    private val currentBasePlaylistIndex: Int
        get() = if (_isShuffleModeEnabled.value) shuffledIndex else linearIndex


    init {
        setupController()
    }

    fun playNow(mediaItem: MediaItem) {
        val player = controller ?: return
        val wasEmpty = player.mediaItemCount == 0

        //Insert media item at the from of manualQueue and rebuild the timeline
        manualQueue.addFirst(mediaItem)
        _manualQueueState.value = manualQueue.toList() //Update UI
        rebuildTimeline(player.currentMediaItem, isStartingNew = false)

        //If something was already playing, force a jump to the new item
        if (!wasEmpty) player.seekTo(player.currentMediaItemIndex + 1, 0L)

        //Force play
        player.play()
    }

    fun playPlaylist(mediaItems: List<MediaItem>, startItemIndex: Int = 0) {
        sourcePlaylist = mediaItems
        manualQueue.clear()

        val startingItem = sourcePlaylist.getOrNull(startItemIndex)

        //Generate shuffledPlaylist variant
        val remaining = sourcePlaylist.filterIndexed { index, _ -> index != startItemIndex }.shuffled()
        shuffledPlaylist = listOfNotNull(startingItem) + remaining

        linearIndex = startItemIndex
        shuffledIndex = if (startingItem != null) 0 else -1

        rebuildTimeline(startingItem, isStartingNew = true)
    }

    fun addToQueue(mediaItems: List<MediaItem>) {
        //Insert media items(s) to the end of manualQueue and rebuild the timeline
        manualQueue.addAll(manualQueue.size, mediaItems)
        _manualQueueState.value = manualQueue.toList() //Update UI
        rebuildTimeline(controller?.currentMediaItem, isStartingNew = false)
    }

    fun clearQueue() {
        //Clear queue and rebuild timeline
        manualQueue.clear()
        _manualQueueState.value = manualQueue.toList() //Update UI
        rebuildTimeline(controller?.currentMediaItem, isStartingNew = false)
    }

    fun moveManualQueueItem(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return

        //Safety check
        if (fromIndex !in manualQueue.indices || toIndex !in manualQueue.indices || fromIndex == toIndex) return

        //Swap in memory
        val temp = manualQueue[fromIndex]
        manualQueue[fromIndex] = manualQueue[toIndex]
        manualQueue[toIndex] = temp

        //Update UI then rebuild timeline
        _manualQueueState.value = manualQueue.toList()
        rebuildTimeline(player.currentMediaItem, isStartingNew = false)
    }

    fun moveBasePlaylistItem(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return
        val currentItem = player.currentMediaItem ?: return

        //Determine which master playlist layout we are actively using
        val activePlaylist = if (_isShuffleModeEnabled.value) shuffledPlaylist else sourcePlaylist
        if (activePlaylist.isEmpty()) return

        //Get the actual indices
        val actualFromIndex = currentBasePlaylistIndex + 1 + fromIndex
        val actualToIndex = currentBasePlaylistIndex + 1 + toIndex

        //Safety check
        if (actualFromIndex !in activePlaylist.indices || actualToIndex !in activePlaylist.indices) return

        //Swap in memory
        val mutableList = activePlaylist.toMutableList()
        val temp = mutableList[actualFromIndex]
        mutableList[actualFromIndex] = mutableList[actualToIndex]
        mutableList[actualToIndex] = temp

        //Save change
        if (_isShuffleModeEnabled.value) shuffledPlaylist = mutableList else sourcePlaylist = mutableList

        //Update UI then rebuild timeline
        val remainingBaseItems = mutableList.subList(currentBasePlaylistIndex + 1, mutableList.size)
        _upNextBaseState.value = remainingBaseItems.toList()
        rebuildTimeline(currentItem, isStartingNew = false)
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
            val anchorItem = current ?: sourcePlaylist.getOrNull(linearIndex)
            val remaining = sourcePlaylist.filter { it.mediaId != anchorItem?.mediaId }.shuffled()
            shuffledPlaylist = listOfNotNull(anchorItem) + remaining
            shuffledIndex = if (anchorItem != null) 0 else -1
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
        val baseIndex = currentBasePlaylistIndex

        //Get everything after current position
        val remainingBaseItems = if (baseIndex != -1 && baseIndex + 1 < basePlaylist.size) {
            basePlaylist.subList(baseIndex + 1, basePlaylist.size)
        } else if (basePlaylist.isNotEmpty() && currentPlayingItem == null) {
            basePlaylist //Nothing is playing yet, so all base items are upcoming
        } else {
            emptyList() //No base playlist active, or we reached the end of it
        }

        //Push to UI
        _manualQueueState.value = manualQueue.toList()
        _upNextBaseState.value = remainingBaseItems.toList()

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
                        var isManualQueueItem = false
                        //Check if we just transitioned into the next manualQueue item - pop if so
                        if (manualQueue.isNotEmpty() && mediaItem.mediaId == manualQueue.first().mediaId) {
                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                                || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                                || reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
                            ) {
                                isManualQueueItem = true

                                //Pop and update UI
                                manualQueue.removeFirst()
                                _manualQueueState.value = manualQueue.toList()
                            }
                        }

                        //Only update tracker if it wasn't a manualQueue item
                        if (!isManualQueueItem) {
                            val activePlaylist = if (_isShuffleModeEnabled.value) shuffledPlaylist else sourcePlaylist
                            val newIndex = activePlaylist.indexOfFirst { it.mediaId == mediaItem.mediaId }
                            if (newIndex != -1) {
                                if (_isShuffleModeEnabled.value) shuffledIndex = newIndex else linearIndex = newIndex
                            }
                        }

                        rebuildTimeline(currentPlayingItem = mediaItem, isStartingNew = false)
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