package com.example.offlineplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import com.example.offlineplayer.ui.components.listitems.QueueItem

@Composable
fun QueueScreen(
    currentlyPlaying: MediaItem?,
    manualQueue: List<MediaItem>,
    upNextBase: List<MediaItem>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onClearQueue: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        //Currently Playing
        currentlyPlaying?.let { current ->
            item {
                //Section Header
                Text(
                    text = "Now Playing",
                    modifier = Modifier.padding(all = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                //Row Item Content
                QueueItem(item = current)
            }
        }

        //Queue
        if (manualQueue.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //Section Header
                    Text(
                        text = "Queue",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    //Clear Queue Option
                    OutlinedButton(onClick = onClearQueue) {
                        Text("Clear Queue")
                    }
                }
            }
            items(manualQueue) { item ->
                //Row Item Content
                QueueItem(item = item)
            }
        }

        //Up Next
        if (upNextBase.isNotEmpty()) {
            item {
                //Section Header
                Text(
                    text = "Up Next",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(upNextBase) { item ->
                //Row Item Content
                QueueItem(item = item)
            }
        }
    }
}