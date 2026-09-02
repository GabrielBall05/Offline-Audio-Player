package com.example.offlineplayer.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaControllerManager @Inject constructor(
    @param:ApplicationContext private val context: Context
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

    private val _isShuffling = MutableStateFlow(false)
    val isShuffling = _isShuffling.asStateFlow()

    private val _repeatModeState = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatModeState = _repeatModeState.asStateFlow()

    private val _manualQueueState = MutableStateFlow<List<MediaItem>>(emptyList())
    val manualQueueState = _manualQueueState.asStateFlow()

    private val _upNextState = MutableStateFlow<List<MediaItem>>(emptyList())
    val upNextState = _upNextState.asStateFlow()

    // Used strictly for restoring the natural order when shuffle is toggled off
    // or for repeating the playlist when the queue ends.
    private var originalPlaylist: List<MediaItem> = emptyList()

    init {
        setupController()
    }

    fun updateCurrentPosition() {
        val player = controller ?: return
        _currentPosition.value = player.currentPosition
        _duration.value = player.duration.coerceAtLeast(0L)
    }

    fun seekToNext() {
        controller?.let {
            it.seekToNext()
            it.play()
        }
    }

    fun seekToPrevious() {
        controller?.let {
            it.seekToPrevious()
            it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun toggleRepeatMode() {
        val player = controller ?: return
        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
        _repeatModeState.value = nextMode
    }

    fun toggleShuffle() {
        val player = controller ?: return
        if (originalPlaylist.isEmpty() || player.mediaItemCount == 0) return

        _isShuffling.value = !_isShuffling.value

        val currentIndex = player.currentMediaItemIndex
        val manualQueueSize = _manualQueueState.value.size
        val upNextStartIndex = currentIndex + 1 + manualQueueSize

        if (upNextStartIndex >= player.mediaItemCount) return // Nothing to shuffle

        // Extract ONLY the up next items (the base playlist items)
        val currentUpNext = mutableListOf<MediaItem>()
        for (i in upNextStartIndex until player.mediaItemCount) {
            currentUpNext.add(player.getMediaItemAt(i))
        }

        val newUpNext = if (_isShuffling.value) {
            // Turning ON: Shuffle the remaining base playlist items
            currentUpNext.shuffled()
        } else {
            // Turning OFF: Restore natural order based on originalPlaylist
            currentUpNext.sortedBy { item ->
                val realId = item.mediaMetadata.extras?.getString("ORIGINAL_MEDIA_ID") ?: item.mediaId
                val index = originalPlaylist.indexOfFirst { it.mediaId == realId }
                if (index != -1) index else Int.MAX_VALUE
            }
        }

        // Seamlessly replace the upcoming base items without touching the current or manual queue items
        player.replaceMediaItems(upNextStartIndex, player.mediaItemCount, newUpNext)
    }

    fun playNow(mediaItem: MediaItem) {
        val player = controller ?: return
        val taggedItem = mediaItem.asManualQueueItem()

        val insertIndex = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
        player.addMediaItem(insertIndex, taggedItem)
        player.seekTo(insertIndex, 0L)
        player.play()
    }

    fun playPlaylist(mediaItems: List<MediaItem>, startItemIndex: Int = -1, startShuffled: Boolean) {
        if (mediaItems.isEmpty()) return
        val player = controller ?: return

        originalPlaylist = mediaItems
        _isShuffling.value = startShuffled

        val startAtSpecific = startItemIndex != -1
        val finalTimeline: List<MediaItem>
        val playIndex: Int

        if (startShuffled) {
            val startIndex = if (startAtSpecific) startItemIndex else mediaItems.indices.random()
            val startingItem = mediaItems[startIndex]
            val remaining = mediaItems.filterIndexed { index, _ -> index != startIndex }.shuffled()

            finalTimeline = listOf(startingItem) + remaining
            playIndex = 0
        } else {
            finalTimeline = mediaItems
            playIndex = if (startAtSpecific) startItemIndex else 0
        }

        player.setMediaItems(finalTimeline)
        player.seekTo(playIndex, 0L)
        player.prepare()
        player.play()
    }

    fun addToQueue(mediaItems: List<MediaItem>) {
        val player = controller ?: return
        if (mediaItems.isEmpty()) return

        val taggedItems = mediaItems.map { it.asManualQueueItem() }

        // Insert right after the last existing manual queue item
        val insertIndex = player.currentMediaItemIndex + 1 + _manualQueueState.value.size
        player.addMediaItems(insertIndex, taggedItems)
    }

    fun clearQueue() {
        val player = controller ?: return
        val currentIndex = player.currentMediaItemIndex

        // Iterate backward through the upcoming items to safely remove manual queue items
        for (i in player.mediaItemCount - 1 downTo currentIndex + 1) {
            val item = player.getMediaItemAt(i)
            if (item.mediaMetadata.extras?.getBoolean("IS_MANUAL_QUEUE") == true) {
                player.removeMediaItem(i)
            }
        }
    }

    fun moveManualQueueItem(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return
        if (fromIndex == toIndex || fromIndex !in _manualQueueState.value.indices || toIndex !in _manualQueueState.value.indices) return

        val actualFrom = player.currentMediaItemIndex + 1 + fromIndex
        val actualTo = player.currentMediaItemIndex + 1 + toIndex

        player.moveMediaItem(actualFrom, actualTo)
    }

    fun moveUpNextItem(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return
        if (fromIndex == toIndex || fromIndex !in _upNextState.value.indices || toIndex !in _upNextState.value.indices) return

        val offset = player.currentMediaItemIndex + 1 + _manualQueueState.value.size
        val actualFrom = offset + fromIndex
        val actualTo = offset + toIndex

        player.moveMediaItem(actualFrom, actualTo)
    }

    fun manualQueueSkipToIndex(index: Int) {
        val player = controller ?: return
        if (index !in _manualQueueState.value.indices) return

        val targetIndex = player.currentMediaItemIndex + 1 + index
        player.seekTo(targetIndex, 0L)
        player.play() // onMediaItemTransition handles consuming the skipped items
    }

    fun upNextSkipToIndex(index: Int) {
        val player = controller ?: return
        if (index !in _upNextState.value.indices) return

        val currentIndex = player.currentMediaItemIndex
        val manualQueueSize = _manualQueueState.value.size

        if (manualQueueSize > 0) {
            // 1. Safely extract the manual queue block
            val queueItems = mutableListOf<MediaItem>()
            val queueStartIndex = currentIndex + 1
            val queueEndIndex = queueStartIndex + manualQueueSize

            for (i in queueStartIndex until queueEndIndex) {
                queueItems.add(player.getMediaItemAt(i))
            }

            // 2. Erase them from their old position (removeMediaItems is exclusive of the end index)
            player.removeMediaItems(queueStartIndex, queueEndIndex)

            // 3. The timeline is now [History] + [Current] + [Up Next]. Calculate true target.
            val newTargetIndex = currentIndex + 1 + index

            // 4. Inject the manual queue exactly ONE slot after the new target song
            player.addMediaItems(newTargetIndex + 1, queueItems)

            // 5. Navigate to the target
            player.seekTo(newTargetIndex, 0L)
        } else {
            // No queue to worry about, seek normally
            val targetIndex = currentIndex + 1 + index
            player.seekTo(targetIndex, 0L)
        }

        player.play()
    }

    fun manualQueueRemoveItemAtIndex(index: Int) {
        val player = controller ?: return
        if (index !in _manualQueueState.value.indices) return

        val actualIndex = player.currentMediaItemIndex + 1 + index
        player.removeMediaItem(actualIndex)
    }

    fun upNextRemoveItemAtIndex(index: Int) {
        val player = controller ?: return
        if (index !in _upNextState.value.indices) return

        val actualIndex = player.currentMediaItemIndex + 1 + _manualQueueState.value.size + index
        player.removeMediaItem(actualIndex)
    }

    fun setupController() {
        if (controllerFuture != null) return

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                val player = controllerFuture?.get() ?: return@addListener
                controller = player

                _currentMediaItem.value = player.currentMediaItem
                _isPlaying.value = player.isPlaying
                _duration.value = player.duration.coerceAtLeast(0L)
                _currentPosition.value = player.currentPosition
                _repeatModeState.value = player.repeatMode

                player.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)

                        _currentMediaItem.value = mediaItem
                        _duration.value = player.duration.coerceAtLeast(0L)

                        consumePastQueueItems(player)
                        updateUIStates(player)
                    }

                    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                        super.onTimelineChanged(timeline, reason)
                        // This guarantees the UI StateFlows sync automatically anytime
                        // items are added, removed, moved, or replaced in ExoPlayer.
                        updateUIStates(player)
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
                Log.e("OfflineAudioSuite", "MCM: Failed to connect", e)
                controllerFuture = null
            }
        }, MoreExecutors.directExecutor())
    }

    /**
     * Rebuilds the UI lists dynamically straight from ExoPlayer's internal timeline.
     */
    private fun updateUIStates(player: Player) {
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex == C.INDEX_UNSET || player.mediaItemCount == 0) {
            _manualQueueState.value = emptyList()
            _upNextState.value = emptyList()
            return
        }

        val queue = mutableListOf<MediaItem>()
        val upNext = mutableListOf<MediaItem>()

        for (i in currentIndex + 1 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            if (item.mediaMetadata.extras?.getBoolean("IS_MANUAL_QUEUE") == true) {
                queue.add(item)
            } else {
                upNext.add(item)
            }
        }

        _manualQueueState.value = queue
        _upNextState.value = upNext
    }

    /**
     * Erases manual queue items from ExoPlayer's history so the "Previous" button functions natively.
     */
    private fun consumePastQueueItems(player: Player) {
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex == C.INDEX_UNSET) return

        // Iterate backward from right behind the currently playing item down to index 0
        for (i in currentIndex - 1 downTo 0) {
            val item = player.getMediaItemAt(i)
            if (item.mediaMetadata.extras?.getBoolean("IS_MANUAL_QUEUE") == true) {
                player.removeMediaItem(i)
            }
        }
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

private fun MediaItem.asManualQueueItem(): MediaItem {
    // Preserve any existing extras, but inject our manual queue identifiers
    val newExtras = Bundle(this.mediaMetadata.extras ?: Bundle()).apply {
        putString("ORIGINAL_MEDIA_ID", this@asManualQueueItem.mediaId)
        putBoolean("IS_MANUAL_QUEUE", true)
    }

    return this.buildUpon()
        .setMediaId("queue_${UUID.randomUUID()}_${this.mediaId}")
        .setMediaMetadata(
            this.mediaMetadata.buildUpon()
                .setExtras(newExtras)
                .build()
        )
        .build()
}