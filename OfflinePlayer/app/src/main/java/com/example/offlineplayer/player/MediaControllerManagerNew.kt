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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaControllerManagerNew @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller: MediaController? = null
        private set

    //Current item state for UI
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
    private val _isShuffling = MutableStateFlow(false)
    val isShuffling = _isShuffling.asStateFlow()

    //Manual queue state for UI
    private val _manualQueueState = MutableStateFlow<List<MediaItem>>(emptyList())
    val manualQueueState = _manualQueueState.asStateFlow()

    //Up next state for UI
    private val _upNextState = MutableStateFlow<List<MediaItem>>(emptyList())
    val upNextState = _upNextState.asStateFlow()

    //Previously playing item to handle manual queue cleanup/consumption
    private var previouslyPlayingItem: MediaItem? = null

    //Current playlist in order
    private var sourcePlaylist: List<MediaItem> = emptyList()

    //Shuffled version of sourcePlaylist
    private var shuffledPlaylist: List<MediaItem> = emptyList()

    //Read only getter for whichever playlist is currently playing (shuffled vs source)
    private val activePlaylist: List<MediaItem>
        get() = if (_isShuffling.value) shuffledPlaylist else sourcePlaylist

    //Manual queue (FIFO)
    private val manualQueue = ArrayDeque<MediaItem>()

    //Timeline for ExoPlayer to see
    private var activeTimeline: List<MediaItem> = emptyList()

    //Index pointers for queues
    private var sourceIndex = 0
    private var shuffledIndex = 0
    //Read only getter for the index of the current playlist (shuffled vs source)

    private val currentIndex: Int
        get() = if (_isShuffling.value) shuffledIndex else sourceIndex


    init {
        setupController()
    }

    fun updateCurrentPosition() {
        val player = controller ?: return
        _currentPosition.value = player.currentPosition
        _duration.value = player.duration.coerceAtLeast(0L)
    }

    fun seekToNext() {
        Log.d("OfflineAudioSuite", "MCM-New: seekToNext")
        val player = controller ?: return
        player.seekToNext()
        player.play() //Force play
    }

    fun seekToPrevious() {
        Log.d("OfflineAudioSuite", "MCM-New: seekToPrevious")
        val player = controller ?: return
        player.seekToPrevious()
        player.play() //Force play
    }

    fun seekTo(positionMs: Long) {
        Log.d("OfflineAudioSuite", "MCM-New: seekTo $positionMs")
        val player = controller ?: return
        player.seekTo(positionMs)
    }

    fun togglePlayPause() {
        Log.d("OfflineAudioSuite", "MCM-New: togglePlayPause")
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun toggleShuffle() {
        Log.d("OfflineAudioSuite", "MCM-New: toggleShuffle")

        if (sourcePlaylist.isEmpty()) return

        //Toggle shuffling state
        _isShuffling.value = !_isShuffling.value

        if (_isShuffling.value) { //If turned shuffle ON:
            //Get currently playing item
            val currentItem = sourcePlaylist.getOrNull(sourceIndex)

            if (currentItem != null) {
                //Reshuffle the shuffled playlist and put the current item in front
                shuffledPlaylist = listOfNotNull(currentItem) +
                        sourcePlaylist.filterIndexed { index, _ -> index != sourceIndex }.shuffled()

                shuffledIndex = 0 //Reset shuffled index
            } else {
                //Fallback to shuffling the whole thing in case of invalid index
                shuffledPlaylist = sourcePlaylist.shuffled()
                shuffledIndex = 0
            }
        } else { //If turned shuffle OFF:
            //Get currently playing item
            val currentItem = shuffledPlaylist.getOrNull(shuffledIndex)

            if (currentItem != null) {
                //Find the current item's natural position in the ordered playlist
                val newSourceIndex = sourcePlaylist.indexOfFirst { it.mediaId == currentItem.mediaId }

                //Update source pointer so Up Next builds from this point
                if (newSourceIndex != -1) sourceIndex = newSourceIndex
            }
        }

        //Apply changes to the active timeline and UI states without interrupting playback
        rebuildTimeline()
    }

    fun playNow(mediaItem: MediaItem) {
        Log.d("OfflineAudioSuite", "MCM-New: Playing ${mediaItem.mediaMetadata.title}, id = ${mediaItem.mediaId}")

        //Clone and tag the item with a UUID
        val taggedItem = mediaItem.asManualQueueItem()

        //Push the item to the front of the manual queue
        manualQueue.addFirst(taggedItem)

        //Rebuild ExoPlayer's timeline
        rebuildTimeline()

        //Safely retrieve player
        val player = controller ?: return

        //Calculate where it is in ExoPlayer's timeline
        val targetIndex = activePlaylist.take(currentIndex + 1).size

        //Force ExoPlayer to skip to it and play
        player.seekTo(targetIndex, 0L)
        player.play()
    }

    fun playPlaylist(mediaItems: List<MediaItem>, startItemIndex: Int = -1, startShuffled: Boolean) {
        Log.d("OfflineAudioSuite", "MCM-New: Playing playlist of size ${mediaItems.size}\n" +
                "Starting at index: $startItemIndex\n" +
                "Starting shuffled: $startShuffled")

        if (mediaItems.isEmpty()) return //Return if given empty list

        //Set shuffling value
        _isShuffling.value = startShuffled

        //Clear manualQueue and initialize source playlist
        manualQueue.clear() //TODO: Maybe retain manual queue
        sourcePlaylist = mediaItems

        //Determine the starting index
        val finalStartIndex = when {
            startItemIndex != -1 -> startItemIndex //User explicitly clicked a song to start at
            startShuffled -> mediaItems.indices.random() //Unspecified start + Shuffling = Random start
            else -> 0 //Unspecified start + No shuffling = Start at beginning
        }

        //Get actual starting item
        val startingItem = sourcePlaylist.getOrNull(finalStartIndex)

        //Shuffled playlist becomes the start item + the remaining items shuffled
        shuffledPlaylist = listOfNotNull(startingItem) +
                sourcePlaylist.filterIndexed { index, _ -> index != finalStartIndex }.shuffled()

        //Assign pointers
        sourceIndex = finalStartIndex //0 if unspecified start, startItemIndex otherwise
        shuffledIndex = 0 //Always the front of the shuffledPlaylist

        //Set activeTimeline to the proper playlist
        activeTimeline = activePlaylist

        //Update UI State Flows
        _currentMediaItem.value = startingItem
        _manualQueueState.value = emptyList()
        //Compute Up Next (everything after current index in the active timeline)
        _upNextState.value = activeTimeline.drop(currentIndex + 1)

        //Safely retrieve the controller
        val player = controller ?: return

        //Log to show controller has been retrieved
        Log.d("OfflineAudioSuite", "MCM-New: Playing " +
                "${if (_isShuffling.value) "Shuffled" else "Ordered"} Playlist, " +
                "and starting at index $currentIndex")

        //Feed to ExoPlayer and play
        player.setMediaItems(activeTimeline)
        player.seekTo(currentIndex, 0L)
        player.prepare()
        player.play()
    }

    fun addToQueue(mediaItems: List<MediaItem>) {
        Log.d("OfflineAudioSuite", "MCM-New: Adding {${mediaItems.size} items to queue")
        if (mediaItems.isEmpty()) return

        //Clone and tag all incoming items
        val taggedItems = mediaItems.map { it.asManualQueueItem() }

        //Append them to the back of the manual queue ArrayDeque
        manualQueue.addAll(taggedItems)

        //Rebuild ExoPlayer's timeline
        rebuildTimeline()
    }

    fun clearQueue() {
        Log.d("OfflineAudioSuite", "MCM-New: Clearing queue")

        val player = controller ?: return
        val currentItem = player.currentMediaItem

        //If a manual queue item is currently playing
        if (currentItem?.localConfiguration?.tag != null) {
            //Get the currently playing manual queue item
            val activeItem = manualQueue.removeFirstOrNull()
            manualQueue.clear() //Clear the manual queue

            //Put the item back so as to not interrupt the audio
            if (activeItem != null) manualQueue.addFirst(activeItem)
        } else {
            //Current media item is from the active playlist, so just clear the manual queue
            manualQueue.clear()
        }

        //Rebuild ExoPlayer's timeline
        rebuildTimeline()
    }

    fun moveManualQueueItem(fromIndex: Int, toIndex: Int) {
        Log.d("OfflineAudioSuite", "MCM-New: moveManualQueueItem: From $fromIndex to $toIndex")

        //Safety check
        if (fromIndex !in manualQueue.indices || toIndex !in manualQueue.indices || fromIndex == toIndex) return

        //Swap the items in memory
        val temp = manualQueue[fromIndex]
        manualQueue[fromIndex] = manualQueue[toIndex]
        manualQueue[toIndex] = temp

        //Rebuild ExoPlayer's timeline
        rebuildTimeline()
    }

    fun moveUpNextItem(fromIndex: Int, toIndex: Int) {
        Log.d("OfflineAudioSuite", "MCM-New: moveUpNextItem: UI indices - from $fromIndex to $toIndex")

        //Map the UI indices to the actual indices in the active playlist
        val actualFrom = fromIndex + currentIndex + 1
        val actualTo = toIndex + currentIndex + 1
        Log.d("OfflineAudioSuite", "MCM-New: moveUpNextItem: Actual indices - from $actualFrom to $actualTo")

        //Safety check
        if (actualFrom !in activePlaylist.indices || actualTo !in activePlaylist.indices || actualFrom == actualTo) return

        //Swap items in memory
        val mutableList = activePlaylist.toMutableList()
        val temp = mutableList[actualFrom]
        mutableList[actualFrom] = mutableList[actualTo]
        mutableList[actualTo] = temp

        //Reassign back to the active playlist
        if (_isShuffling.value) shuffledPlaylist = mutableList
        else sourcePlaylist = mutableList

        //Rebuild ExoPlayer's timeline
        rebuildTimeline()
    }

    fun manualQueueSkipToIndex(index: Int) {
        Log.d("OfflineAudioSuite", "MCM-New: manualQueueSkipToIndex: $index")

        val player = controller ?: return

        //Safety check
        if (index !in manualQueue.indices) return

        //Remove all manual queue items preceding the target item
        repeat(index) { manualQueue.removeFirstOrNull() }

        //Rebuild ExoPlayer's timeline with the trimmed manual queue
        rebuildTimeline()

        //Target item is now the first item in the manual queue which is right after the history - skip to it
        player.seekTo(currentIndex + 1, 0L)
        player.play()
    }

    fun upNextSkipToIndex(index: Int) {
        Log.d("OfflineAudioSuite", "MCM-New: upNextSkipToIndex: UI Index $index")

        val player = controller ?: return

        val targetTimelineIndex = (currentIndex + 1) + manualQueue.size + index
        Log.d("OfflineAudioSuite", "MCM-New: upNextSkipToIndex: Actual index $targetTimelineIndex")

        //Safety check
        if (targetTimelineIndex !in 0 until player.mediaItemCount) return

        //Seek directly to item
        player.seekTo(targetTimelineIndex, 0L)
        player.play()
    }

    fun manualQueueRemoveItemAtIndex(index: Int) {
        Log.d("OfflineAudioSuite", "MCM-New: manualQueueRemoveItemAtIndex: $index")

        //Safety check
        if (index !in manualQueue.indices) return

        //Remove it from the manual queue
        manualQueue.removeAt(index)

        //Rebuild ExoPlayer's timeline
        rebuildTimeline()
    }

    fun upNextRemoveItemAtIndex(index: Int) {
        Log.d("OfflineAudioSuite", "MCM-New: upNextRemoveItemAtIndex: UI Index $index")

        //Map the UI index to the actual index in the active playlist
        val actualIndex = index + currentIndex + 1
        Log.d("OfflineAudioSuite", "MCM-New: upNextRemoveItemAtIndex: Actual Index $actualIndex")

        //Safety check
        if (actualIndex !in activePlaylist.indices) return

        //Mutable copy of the active playlist to remove the target item
        val mutableList = activePlaylist.toMutableList()
        mutableList.removeAt(actualIndex)

        //Reassign back to appropriate active playlist
        if (_isShuffling.value) shuffledPlaylist = mutableList
        else sourcePlaylist = mutableList

        //Rebuild ExoPlayer's timeline
        rebuildTimeline()
    }

    private fun rebuildTimeline() {
        Log.d("OfflineAudioSuite", "MCM-New: Rebuilding Timeline")

        val player = controller ?: return

        //Define boundary between history/present and future
        val historyBoundary = currentIndex + 1

        //Split active playlist at the boundary
        val historyAndCurrent = activePlaylist.take(historyBoundary)
        val futurePlaylist = activePlaylist.drop(historyBoundary)

        //Get manual queue ArrayDeque as list
        val manualQueueList = manualQueue.toList()

        //Assemble new active timeline
        activeTimeline = historyAndCurrent + manualQueueList + futurePlaylist

        //Update UI State Flows
        _manualQueueState.value = manualQueueList
        _upNextState.value = futurePlaylist

        //Sync ExoPlayer without interrupting playback
        player.replaceMediaItems(0, player.mediaItemCount, activeTimeline)
    }

    fun setupController() {
        if (controllerFuture != null) return

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                val player = controllerFuture?.get() ?: return@addListener
                controller = player

                //Sync initial state from the connected session
                _currentMediaItem.value = player.currentMediaItem
                _isPlaying.value = player.isPlaying
                _duration.value = player.duration.coerceAtLeast(0L)
                _currentPosition.value = player.currentPosition

                //Attach listener to track state changes
                player.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        Log.d("OfflineAudioSuite", "MCM-New: onMediaItemTransition\n" +
                                "Transitioned to: Title = ${mediaItem?.mediaMetadata?.title}, ID = ${mediaItem?.mediaId}\n" +
                                "Reason: ${getTransitionReasonString(reason)}")

                        //Update current item and duration for UI
                        _currentMediaItem.value = mediaItem
                        _duration.value = player.duration.coerceAtLeast(0L)

                        //Start logic for manual queue item cleanup and indice shifting
                        var needsRebuild = false

                        //Handle previous item cleanup to consume manual queue items
                        val prevItem = previouslyPlayingItem
                        if (prevItem != null && prevItem.localConfiguration?.tag != null) {
                            //If it has a tag, it was a manual queue item - Remove it
                            val removed = manualQueue.remove(prevItem)
                            if (removed) {
                                Log.d("OfflineAudioSuite", "MCM-New: Consumed manual queue item")
                                needsRebuild = true
                            }
                        }

                        //Update indices if the new item is an active playlist item
                        mediaItem?.let {
                            if (mediaItem.localConfiguration?.tag == null) {
                                //If it has no tag, it is an active playlist item - Update relevant index
                                if (_isShuffling.value) shuffledIndex = shuffledPlaylist.indexOfFirst { it.mediaId == mediaItem.mediaId }
                                else sourceIndex = sourcePlaylist.indexOfFirst { it.mediaId == mediaItem.mediaId }

                                needsRebuild = true
                            }
                        }

                        //Rebuild ExoPlayer's timeline if needed
                        if (needsRebuild) rebuildTimeline()

                        //Track current item for the next transition
                        previouslyPlayingItem = mediaItem
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        _isPlaying.value = isPlaying
                        Log.d("OfflineAudioSuite", "MCM-New: isPlaying changed to: ${_isPlaying.value}")
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = player.duration.coerceAtLeast(0L)
                            Log.d("OfflineAudioSuite", "MCM-New: duration changed to: ${_duration.value}")
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

private fun MediaItem.asManualQueueItem(): MediaItem {
    return this.buildUpon()
        //Tag the media item with a UUID so it can be identified later
        .setTag(UUID.randomUUID().toString())
        .build()
}

private fun getTransitionReasonString(reason: Int): String {
    val reasonString = when (reason) {
        Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "MEDIA_ITEM_TRANSITION_REASON_AUTO"
        Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "MEDIA_ITEM_TRANSITION_REASON_SEEK"
        Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED"
        Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "MEDIA_ITEM_TRANSITION_REASON_REPEAT"
        else -> "UNKNOWN_REASON"
    }
    return reasonString
}