package com.example.offlineplayer.ui.screens

import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.offlineplayer.ui.viewmodels.MainViewModel
import com.example.offlineplayer.util.KeepScreenOn
import kotlinx.coroutines.launch

@Composable
fun ExpandedPlayerScreen(
    viewModel: MainViewModel,
    onCollapse: () -> Unit
) {
    KeepScreenOn() //Call helper composable to ensure the screen stays on while this screen/composable is active

    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    //Slider states
    var sliderPosition by remember { mutableFloatStateOf(0F) }
    var isDragging by remember { mutableStateOf(false) }

    //Sync slider with actual position unless user is dragging it
    LaunchedEffect(currentPosition) { if (!isDragging) sliderPosition = currentPosition.toFloat() }

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
                .padding(horizontal = 8.dp),
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
                text = "Playing", //TODO: Show a good title for this, ex: playlist name if playing from a playlist
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

        //Artwork Placeholder
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .aspectRatio(1f),
            shape = RoundedCornerShape(8.dp),
            color = Color.Gray
        ) { /*PUT IMAGE HERE OR REPLACE SURFACE WITH IMAGE*/ }

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
                Column(
                    modifier = Modifier.weight(1f)) {
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
                        .size(46.dp)
                        .padding(horizontal = 8.dp)
                        .aspectRatio(1f),
                    onClick = { viewModel.onRepeatModeClicked() }
                ) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat Mode"
                    )
                }

                //Add to Playlist Button
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = {
                        currentMediaItem?.mediaId?.toIntOrNull()?.let { id ->
                            viewModel.onAddToPlaylistClicked(id)
                        }
                    }
                ) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "Add to Playlist"
                    )
                }
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
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
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
                //Play Mode (Shuffle vs Order)
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = { viewModel.onPlayModeClicked() }
                ) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Play Mode"
                    )
                }

                //Grouped Playback Controls (Prev, Toggle, Next)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    //Previous Button
                    IconButton(
                        modifier = Modifier.size(48.dp),
                        onClick = { viewModel.seekToPrevious() }
                    ) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous"
                        )
                    }

                    //Play/Pause Toggle
                    IconButton(
                        modifier = Modifier.size(80.dp),
                        onClick = { viewModel.togglePlayPause() }
                    ) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = "Play/Pause Toggle"
                        )
                    }

                    //Next Button
                    IconButton(
                        modifier = Modifier.size(48.dp),
                        onClick = { viewModel.seekToNext() }
                    ) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next"
                        )
                    }
                }

                //Queue
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = { /* TODO: Show Queue */ }
                ) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                        //imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = "Queue"
                    )
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}