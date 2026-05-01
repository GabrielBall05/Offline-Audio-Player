package com.example.offlineplayer.ui.components.listitems

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
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
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.ui.components.common.SelectionIcon

@Composable
fun MediaListItemSimple(
    media: MediaEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Artwork Placeholder
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.LightGray
        ) { Icon(Icons.Default.MusicNote, contentDescription = "Artwork Image") }

        Spacer(modifier = Modifier.width(12.dp))

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
            )
            //Creator
            Text(
                text = media.creator,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        //Checkbox
        SelectionIcon(isSelected = isSelected)
    }
}