package com.example.offlineplayer.ui.components.listitems

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.ui.components.common.InfoColumnMarquee
import com.example.offlineplayer.ui.components.common.SurfacedImage

@Composable
fun MediaListItemReorderable(
    media: MediaEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        //Reorder Buttons
        IconButton(onClick = onMoveUp, enabled = !isFirst) {
            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onMoveDown, enabled = !isLast) {
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = MaterialTheme.colorScheme.primary)
        }
    }
}