package com.example.offlineplayer.ui.components.listitems

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.offlineplayer.data.MediaEntity
import com.example.offlineplayer.ui.components.common.SelectionIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaListItem(
    media: MediaEntity,
    isSelected: Boolean,
    onCheckBoxClick: () -> Unit,
    onMoreClick: (MediaEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Selection Checkbox
        IconButton(onClick = onCheckBoxClick) {
            SelectionIcon(isSelected)
        }

        //Artwork Placeholder
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.LightGray
        ) {
            //PLACEHOLDER IMAGE
            Icon(Icons.Default.MusicNote, contentDescription = "Artwork Image")
        }

        //Media Item Info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            //Title
            Text(
                text = media.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
            //Creator
            Text(
                text = media.creator,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
        }

        //More Button (ellipsis) - brings up menu for edit, play, add to queue, add to playlist, delete, etc.
        IconButton(onClick = { onMoreClick(media) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.Gray)
        }
    }
}