package com.example.offlineplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ShuffleOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.offlineplayer.ui.components.common.SurfacedImage
import com.example.offlineplayer.ui.viewmodels.MainViewModel
import com.example.offlineplayer.util.KeepScreenOn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayerScreen(
    viewModel: MainViewModel,
    onCollapse: () -> Unit
) {
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle() //User's keep screen on setting
    if (keepScreenOn) KeepScreenOn() //Call helper composable to ensure the screen stays on while this screen/composable is active

    //Collect states from viewmodel
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isRepeatingCurrent by viewModel.isRepeatingCurrent.collectAsStateWithLifecycle()
    val isShuffleModeEnabled by viewModel.isShuffleModeEnabled.collectAsStateWithLifecycle()
    val manualQueue by viewModel.manualQueue.collectAsStateWithLifecycle()
    val upNext by viewModel.upNext.collectAsStateWithLifecycle()

    var showQueueScreen by rememberSaveable { mutableStateOf(false) }

    //Sheet state
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    //Slider states
    var sliderPosition by remember { mutableFloatStateOf(0F) }
    var isDragging by remember { mutableStateOf(false) }

    //Sync slider with actual position unless user is dragging it
    LaunchedEffect(currentPosition) { if (!isDragging) sliderPosition = currentPosition.toFloat() }


    //Screen UI
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = 100.dp)
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            //Back button, Title, Options menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Back button
                IconButton(onClick = onCollapse) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse"
                    )
                }

                //Title
                Text(
                    text = "Player", //TODO: Show a good title for this, ex: playlist name if playing from a playlist
                    style = MaterialTheme.typography.titleLarge
                )

                //Options Menu
                IconButton(onClick = { /* TODO: Set some var to open a menu sheet for something idk */ }) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options"
                    )
                }
            }

            //Artwork
            SurfacedImage(
                model = currentMediaItem?.mediaMetadata?.artworkUri?.toString(),
                contentDescription = "Artwork Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .aspectRatio(1f),
            )

            //Information + Slider + Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                //Title + Creator + Repeat Mode + Add To Playlist Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        //Title
                        Text(
                            text = currentMediaItem?.mediaMetadata?.title?.toString() ?: "Unknown Title",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        //Creator
                        Text(
                            text = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Creator",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }

                    //Repeat Button
                    IconButton(
                        modifier = Modifier
                            .size(52.dp)
                            .aspectRatio(1f),
                        onClick = { viewModel.toggleRepeatMode() }
                    ) {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            imageVector = if (isRepeatingCurrent) Icons.Default.RepeatOn else Icons.Default.Repeat,
                            tint = if (isRepeatingCurrent) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            contentDescription = "Repeat Mode"
                        )
                    }

//                    //Add to Playlist Button
//                    IconButton(
//                        modifier = Modifier.size(32.dp),
//                        onClick = {
//                            //Look for the hidden original ID first, fallback to the standard mediaId if it's a base playlist item
//                            val realIdString = currentMediaItem?.mediaMetadata?.extras?.getString("ORIGINAL_MEDIA_ID")
//                                ?: currentMediaItem?.mediaId
//
//                            realIdString?.toIntOrNull()?.let { id ->
//                                viewModel.onAddToPlaylistClicked(id)
//                            }
//                        }
//                    ) {
//                        Icon(
//                            modifier = Modifier.fillMaxSize(),
//                            imageVector = Icons.Default.AddCircleOutline,
//                            contentDescription = "Add to Playlist"
//                        )
//                    }
                }

                //Slider + Times
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        //Seek Slider
                        Slider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp),
                            value = sliderPosition,
                            onValueChange = {
                                isDragging = true
                                sliderPosition = it
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                viewModel.seekTo(sliderPosition.toLong())
                            },
                            valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
                            thumb = {
                                Box(modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(4.dp),
                                    thumbTrackGapSize = 0.dp,
                                    drawStopIndicator = null
                                )
                            }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-10).dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            //Duration Texts
                            Text(text = formatTime(sliderPosition.toLong()), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            Text(text = formatTime(duration), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }

                //Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
//                    //Shuffle Toggle Button
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.toggleShuffle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            imageVector = if(isShuffleModeEnabled) Icons.Rounded.ShuffleOn else Icons.Rounded.Shuffle,
                            tint = if(isShuffleModeEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            contentDescription = "Shuffle Toggle"
                        )
                    }

                    //Grouped Playback Controls (Prev, Toggle, Next)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        //Previous Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.seekToPrevious() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous"
                            )
                        }

                        //Play/Pause Toggle
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null //Remove ripple effect when clicked
                                ) { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Play/Pause Toggle"
                            )
                        }

                        //Next Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.seekToNext() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next"
                            )
                        }
                    }

                    //Queue Button
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { showQueueScreen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = "Open Queue"
                        )
                    }
                }
            }
        }

        //Show Queue screen if user hits the queue button
        if (showQueueScreen) {
            ModalBottomSheet(
                onDismissRequest = { showQueueScreen = false },
                sheetState = queueSheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)) {
                    QueueScreen(
                        currentlyPlaying = currentMediaItem,
                        manualQueue = manualQueue,
                        upNext = upNext,
                        onDismiss = { showQueueScreen = false },
                        onClearQueue = { viewModel.clearQueue() },
                        onMoveManualQueueItem = { from, to ->
                            viewModel.moveManualQueueItem(from, to)
                        },
                        onMoveUpNextItem = { from, to ->
                            viewModel.moveUpNextItem(from, to)
                        },
                        onManualQueueSkipToIndex = { viewModel.manualQueueSkipToIndex(it) },
                        onUpNextSkipToIndex = { viewModel.upNextSkipToIndex(it) },
                        onManualQueueRemoveItemAtIndex = { viewModel.manualQueueRemoveItemAtIndex(it) },
                        onUpNextRemoveItemAtIndex = { viewModel.upNextRemoveItemAtIndex(it) }
                    )
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}