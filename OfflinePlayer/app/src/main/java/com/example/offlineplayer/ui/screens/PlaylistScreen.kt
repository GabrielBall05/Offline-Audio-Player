package com.example.offlineplayer.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.offlineplayer.data.PlaylistEntity
import com.example.offlineplayer.ui.components.common.FilterButton
import com.example.offlineplayer.ui.components.common.SearchBar
import com.example.offlineplayer.ui.components.dialogs.CreatePlaylistDialog
import com.example.offlineplayer.ui.components.dialogs.DeleteConfirmationDialog
import com.example.offlineplayer.ui.components.listitems.PlaylistListItem
import com.example.offlineplayer.ui.viewmodels.PlaylistsViewModel

@Composable
fun PlaylistScreen(viewModel: PlaylistsViewModel = hiltViewModel()) { //Let Hilt inject the ViewModel
    //Collect states from ViewModel
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val playlistList by viewModel.filteredPlaylists.collectAsStateWithLifecycle()

    var creatingPlaylist by remember { mutableStateOf(false) }
    var selectedPlaylistForMenu by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToEdit by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }


    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            //Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Add or Edit Playlists",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            //Search + Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Search
                SearchBar(
                    value = searchQuery,
                    placeHolderText = "Search playlists...",
                    modifier = Modifier.weight(1f),
                    onValueChange = { viewModel.onSearchQueryChange(it) }
                )

                //Filter
                FilterButton(onClick = { /* TODO: Open Filter/Sort Dialog for All Playlists List */ })
            }

            //Playlist List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp)
            ) {
                items(
                    items = playlistList,
                    key = { it.playlistId }
                ) { playlist ->
                    PlaylistListItem(
                        playlist = playlist,
                        onMoreClick = { selectedPlaylistForMenu = playlist }
                    )
                }
            }
        }

        //Create Playlist Button
        FloatingActionButton(
            onClick = { creatingPlaylist = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Playlist")
        }
    }


    //Show CreatePlaylistDialog if user clicks FAB
    if (creatingPlaylist) {
        CreatePlaylistDialog(
            onDismiss = { creatingPlaylist = false },
            onConfirm = { playlist ->
                viewModel.createPlaylist(playlist)
                creatingPlaylist = false
            }
        )
    }

    //Show options menu if user hits ellipses on playlist
    selectedPlaylistForMenu?.let { playlist ->
        //TODO: Create a PlaylistOptionsSheet composable and invoke it in a ModalBottomSheet in here
    }

    //Show edit dialog if user hits edit
    playlistToEdit?.let { playlist ->
        //TODO: Create an EditPlaylistDialog composable and invoke it in here
    }

    //Show delete dialog if user hits delete
    playlistToDelete?.let { playlist ->
        DeleteConfirmationDialog(
            title = "Delete Playlist",
            text = "Are you sure you want to delete the playlist \"${playlist.name}\"? This action cannot be undone.",
            onConfirm = {
                viewModel.deletePlaylist(playlist)
                playlistToDelete = null
            },
            onDismiss =  {
                playlistToDelete = null
            }
        )
    }
}