package com.example.offlineplayer.ui.components.playlists

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.offlineplayer.data.PlaylistEntity

@Composable
fun PlaylistListItem(
    playlist: PlaylistEntity,
    onMoreClick: (PlaylistEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
       verticalAlignment = Alignment.CenterVertically
    ) {
        //Artwork Placeholder
        Surface(
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.LightGray
        ) {
            //PLACEHOLDER IMAGE
            Icon(Icons.Default.LibraryMusic, contentDescription = "Artwork Image")
            //Icon(Icons.Default.Album, contentDescription = "Artwork Image")
        }

        //Name + Description
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            //Name
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )

            //Description
            Text(
                text = playlist.description ?: "x items",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
        }

        //More Button - Brings up menu for things like delete
        IconButton(onClick = { onMoreClick(playlist) }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.Gray)
        }
    }
}