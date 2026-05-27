package com.example.offlineplayer.ui.components.listitems

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import com.example.offlineplayer.ui.components.common.SurfacedImage

@Composable
fun QueueItem(
    item: MediaItem,
    modifier: Modifier = Modifier,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Artwork
        SurfacedImage(
            model = item.mediaMetadata.artworkUri.toString(),
            contentDescription = "Queue Item Artwork",
            sizeInDp = 50.dp
        )

        //Title + Creator
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            //Title
            Text(
                text = item.mediaMetadata.title.toString(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )

            //Creator
            Text(
                text = item.mediaMetadata.artist.toString(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
        }

        //Reordering Buttons
        if (!(isFirst && isLast)) {
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}