package com.example.offlineplayer.ui.components.listitems

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.ui.components.common.InfoColumnMarquee
import com.example.offlineplayer.ui.components.common.SelectionIcon
import com.example.offlineplayer.ui.components.common.SurfacedImage

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

        //Artwork
        SurfacedImage(
            model = media.artworkUri,
            contentDescription = "Artwork Image"
        )

        //Media Item Info
        InfoColumnMarquee(
            mainText = media.title,
            subText = media.creator
        )

        //More Button (ellipsis) - brings up menu for edit, play, add to queue, add to playlist, delete, etc.
        IconButton(onClick = { onMoreClick(media) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.Gray)
        }
    }
}