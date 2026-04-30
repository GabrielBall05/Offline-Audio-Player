package com.example.offlineplayer.ui.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.offlineplayer.data.PlaylistEntity
import com.example.offlineplayer.ui.components.listitems.PlaylistListItemSimple

@Composable
fun PlaylistPicker(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    val selectedPlaylistIds = remember { mutableStateListOf<Int>() }
    //TODO: Already select playlists that they're already in. Also allow for removing from those playlists
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Playlist(s)") },
        text = {
            if (playlists.isEmpty()) Text("No playlists found. Create one here") //TODO: Provide shortcut to PlaylistFormDialog
            else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(
                        items = playlists,
                        key = { it.playlistId }
                    ) { playlist ->
                        PlaylistListItemSimple(
                            playlist = playlist,
                            isSelected = selectedPlaylistIds.contains(playlist.playlistId),
                            onClick = {
                                if (selectedPlaylistIds.contains(playlist.playlistId)) selectedPlaylistIds.remove(playlist.playlistId)
                                else selectedPlaylistIds.add(playlist.playlistId)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedPlaylistIds.isNotEmpty(),
                onClick = { onConfirm(selectedPlaylistIds.toList()) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}