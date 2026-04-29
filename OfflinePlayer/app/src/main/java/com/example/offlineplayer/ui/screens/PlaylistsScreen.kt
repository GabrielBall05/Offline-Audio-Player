package com.example.offlineplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.offlineplayer.data.PlaylistEntity
import com.example.offlineplayer.ui.Screen
import com.example.offlineplayer.ui.components.common.SearchBar
import com.example.offlineplayer.ui.components.dialogs.DeleteConfirmationDialog
import com.example.offlineplayer.ui.components.dialogs.PlaylistFormDialog
import com.example.offlineplayer.ui.components.dialogs.SortOrderDialog
import com.example.offlineplayer.ui.components.listitems.PlaylistListItem
import com.example.offlineplayer.ui.components.optionsheets.PlaylistOption
import com.example.offlineplayer.ui.components.optionsheets.PlaylistOptionsSheet
import com.example.offlineplayer.ui.viewmodels.PlaylistsViewModel
import com.example.offlineplayer.util.PlaylistSortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    navController: NavController,
    viewModel: PlaylistsViewModel = hiltViewModel()
) { //Let Hilt inject the ViewModel
    //Collect states from ViewModel
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val playlistList by viewModel.filteredPlaylists.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState()

    var creatingPlaylist by remember { mutableStateOf(false) }
    var playlistToAddMedia by remember { mutableStateOf<PlaylistEntity?>(null) }
    var selectedPlaylistForMenu by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToEdit by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }

    //Jump to top of list when list size changes or sort order is changed
    LaunchedEffect(playlistList.size, sortOrder) {
        if (playlistList.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

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
                    onClear = { viewModel.onSearchQueryChange("") },
                    onValueChange = { viewModel.onSearchQueryChange(it) }
                )

                //Sort
                IconButton(onClick = { showSortDialog = true }) {
                    Icon(Icons.AutoMirrored.Default.Sort, contentDescription = "Sort List")
                }
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
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.PlaylistDetails.createRoute(playlist.playlistId))
                        },
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

    //Show MediaPicker if user clicks Add Media
    playlistToAddMedia?.let {
        //TODO: Create MediaPicker and invoke here
    }

    //Show PlaylistFormDialog if user clicks FAB
    if (creatingPlaylist) {
        PlaylistFormDialog(
            onDismiss = { creatingPlaylist = false },
            onConfirm = { playlist ->
                viewModel.createPlaylist(playlist)
                creatingPlaylist = false
            }
        )
    }

    //Show options menu if user hits ellipses on playlist
    selectedPlaylistForMenu?.let { playlist ->
        ModalBottomSheet(
            onDismissRequest = { selectedPlaylistForMenu = null },
            sheetState = sheetState
        ) {
            PlaylistOptionsSheet(
                playlist = playlist,
                onOptionClick = { option ->
                    selectedPlaylistForMenu = null
                    when (option) {
                        PlaylistOption.EDIT -> { playlistToEdit = playlist }
                        PlaylistOption.PLAY_NOW -> { viewModel.playPlaylistById(playlist.playlistId) }
                        PlaylistOption.ADD_MEDIA -> { playlistToAddMedia = playlist }
                        PlaylistOption.DELETE -> { playlistToDelete = playlist }
                    }
                }
            )
        }
    }

    //Show PlaylistFormDialog if user clicks Edit
    playlistToEdit?.let {
        PlaylistFormDialog(
            playlistToEdit = playlistToEdit,
            onDismiss = { playlistToEdit = null },
            onConfirm = { playlist ->
                viewModel.editPlaylist(playlist)
                playlistToEdit = null
            }
        )
    }

    //Show SortOrderDialog if user clicks Sort button
    if (showSortDialog) {
        SortOrderDialog(
            title = "Sort Playlists By",
            options = PlaylistSortOrder.entries.toTypedArray(),
            currentSelection = sortOrder,
            onDismiss = { showSortDialog = false },
            onOptionSelected = { option ->
                showSortDialog = false
                viewModel.onSortOrderChange(option)
            }
        )
    }

    //Show delete dialog if user hits delete
    playlistToDelete?.let { playlist ->
        DeleteConfirmationDialog(
            title = "Delete Playlist: \"${playlist.name}\"?",
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