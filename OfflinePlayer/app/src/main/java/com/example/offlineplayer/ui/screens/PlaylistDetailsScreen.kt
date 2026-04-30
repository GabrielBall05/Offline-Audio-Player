package com.example.offlineplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.offlineplayer.data.MediaEntity
import com.example.offlineplayer.ui.components.common.BulkActionsBar
import com.example.offlineplayer.ui.components.common.SearchBar
import com.example.offlineplayer.ui.components.listitems.MediaListItem
import com.example.offlineplayer.ui.viewmodels.PlaylistDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    onBack: () -> Unit,
    viewModel: PlaylistDetailsViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val mediaList by viewModel.filteredMedia.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isAnySelected by viewModel.isAnySelected.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState()

    var idsToRemove by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var idsToAddToAnotherPlaylist by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var selectedMediaItemForMenu by remember { mutableStateOf<MediaEntity?>(null) }
    var mediaToEdit by remember { mutableStateOf<MediaEntity?>(null) }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showPlaylistOptionsSheet by remember { mutableStateOf(false) }
    var showEditPlaylistDialog by remember { mutableStateOf(false) }


    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            //TODO: Put artwork somewhere.

            //Back button, Title, Options menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Back button
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                }

                //Title
                Text(text = playlist?.name ?: "Playlist Details", style = MaterialTheme.typography.titleLarge)

                //Options Menu
                IconButton(onClick = { /* TODO: Open the same ModalBottomSheet as clicking more on a PlaylistListItem would */ }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                }
            }

            //Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    value = searchQuery,
                    placeHolderText = "Search in playlist",
                    modifier = Modifier.fillMaxWidth(),
                    onClear = { viewModel.onSearchQueryChange("") },
                    onValueChange = { viewModel.onSearchQueryChange(it) }
                )
            }

            //Bulk Actions
            BulkActionsBar(
                isAnySelected = isAnySelected,
                isAllSelected = isAllSelected,
                onToggleAllClick = { viewModel.toggleSelectAll() },
                onClearSelectionClick = { viewModel.clearSelection() }
            ) {
                IconButton(onClick = { idsToAddToAnotherPlaylist = selectedIds.toList() }) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add To Another Playlist")
                }
                IconButton(onClick = { idsToRemove = selectedIds.toList() }) {
                    Icon(Icons.Default.PlaylistRemove, contentDescription = "Remove From Playlist")
                }
            }

            //Media List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp)
            ) {
                items(
                    items = mediaList,
                    key = { it.mediaId }
                ) { media ->
                    MediaListItem(
                        media = media,
                        isSelected = selectedIds.contains(media.mediaId),
                        onCheckBoxClick = { viewModel.toggleSelection(media.mediaId) },
                        onMoreClick = { selectedMediaItemForMenu = media }
                    )
                }
            }
        }

        //Add Media To Playlist Button
        FloatingActionButton(
            onClick = { showMediaPicker = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Media To Playlist")
        }
    }

    
    //Show PlaylistFormDialog if user wants to edit this playlist
    if (showEditPlaylistDialog) {

    }

    //Show ConfirmationDialog if user wants to remove media items from this playlist
    if (idsToRemove.isNotEmpty()) {

    }

    //Show ModalBottomSheet options for a media item if user clicks ellipsis on that item
    selectedMediaItemForMenu?.let {

    }

    //Show EditMediaDialog if user wants to edit a media item from here
    mediaToEdit?.let {

    }

    //Show MediaPicker if user wants to add media to this playlist from here
    if (showMediaPicker) {

    }

    //Show ModalBottomSheet options for this playlist if user clicks the ellipsis at the top right
    if (showPlaylistOptionsSheet) {

    }

    //Show PlaylistPicker if user wants to add items to another playlist from here
    if (idsToAddToAnotherPlaylist.isNotEmpty()) {

    }
}