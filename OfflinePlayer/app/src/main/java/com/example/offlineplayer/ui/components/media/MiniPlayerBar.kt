package com.example.offlineplayer.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.offlineplayer.player.MediaControllerManager
import com.example.offlineplayer.ui.viewmodels.MainViewModel

@Composable
fun MiniPlayerBar(
    viewModel: MainViewModel,
    onExpand: () -> Unit //To open full-screen player
) {
    val currentMedia by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    //If nothing is loaded in the player (no media items), don't show the bar at all
    if (currentMedia == null) return

    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    //This Box allows me to layer the Progress Bar at the very bottom
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onExpand)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Artwork Placeholder
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Gray
            ) { /*PUT IMAGE HERE OR REPLACE SURFACE WITH IMAGE*/ }

            //Title & Creator
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                //Title
                Text(
                    text = currentMedia?.mediaMetadata?.title?.toString() ?: "Unknown Title",
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                //Creator
                Text(
                    text = currentMedia?.mediaMetadata?.artist?.toString() ?: "Unknown Creator",
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }

            //Controls
            IconButton(onClick = { viewModel.seekToPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Track")
            }
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause")
            }
            IconButton(onClick = { viewModel.seekToNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next Track")
            }
        }

        //Progress Line
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(3.dp), //Very thin line
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
    }
}