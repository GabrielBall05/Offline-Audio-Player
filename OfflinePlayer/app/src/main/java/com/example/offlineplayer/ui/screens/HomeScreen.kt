package com.example.offlineplayer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.ui.components.common.BulkActionsBar
import com.example.offlineplayer.ui.components.common.SearchBar
import com.example.offlineplayer.ui.components.listitems.MediaListItem
import com.example.offlineplayer.ui.components.dialogs.ConfirmationDialog
import com.example.offlineplayer.ui.components.dialogs.EditMediaDialog
import com.example.offlineplayer.ui.components.dialogs.PlaylistPicker
import com.example.offlineplayer.ui.components.dialogs.SortOrderDialog
import com.example.offlineplayer.ui.components.optionsheets.MediaOption
import com.example.offlineplayer.ui.components.optionsheets.MediaOptionsSheetContent
import com.example.offlineplayer.ui.viewmodels.HomeViewModel
import com.example.offlineplayer.util.MediaSortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) { //Let Hilt inject the ViewModel
    //Collect states from ViewModel
    val mediaList by viewModel.filteredMedia.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isAnySelected by viewModel.isAnySelected.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState()

    var idsToDelete by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var idsToAddToPlaylists by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var selectedMediaItemForMenu by remember { mutableStateOf<MediaEntity?>(null) }
    var mediaToEdit by remember { mutableStateOf<MediaEntity?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }

    //File Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenMultipleDocuments()) {
        uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMedia(uris)
        }
    }

    //Jump to top of list when list size changes or sort order is changed
    LaunchedEffect(mediaList.size, sortOrder) {
        if (mediaList.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            //Page Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Add or Edit Media",
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
                //Search Bar
                SearchBar(
                    value = searchQuery,
                    placeHolderText = "Search media...",
                    modifier = Modifier.weight(1f),
                    onClear = { viewModel.onSearchQueryChange("") },
                    onValueChange = { viewModel.onSearchQueryChange(it) }
                )

                //Sort
                IconButton(onClick = { showSortDialog = true }) {
                    Icon(Icons.AutoMirrored.Default.Sort, contentDescription = "Sort List")
                }
            }

            //Bulk Actions
            BulkActionsBar(
                isAnySelected = isAnySelected,
                isAllSelected = isAllSelected,
                onToggleAllClick = { viewModel.toggleSelectAll() },
                onClearSelectionClick = { viewModel.clearSelection() }
            ) {
                IconButton(onClick = { idsToAddToPlaylists = selectedIds.toList() }) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add To Playlist")
                }
                IconButton(onClick = { idsToDelete = selectedIds.toList() }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete")
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

        //Upload Media Button (FAB)
        FloatingActionButton(
            onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Upload Media")
        }
    }


    //Show options menu if user hits ellipsis on a media item
    selectedMediaItemForMenu?.let { media ->
        ModalBottomSheet(
            onDismissRequest = { selectedMediaItemForMenu = null },
            sheetState = sheetState
        ) {
            MediaOptionsSheetContent(
                media = media,
                showDeleteOption = true,
                onOptionClick = { option ->
                    selectedMediaItemForMenu = null
                    when (option) {
                        MediaOption.EDIT -> mediaToEdit = media
                        MediaOption.PLAY_NOW -> viewModel.playMedia(media)
                        MediaOption.ADD_TO_QUEUE -> viewModel.addMediaToQueue(media)
                        MediaOption.ADD_TO_PLAYLIST -> idsToAddToPlaylists = listOf(media.mediaId)
                        MediaOption.REMOVE_FROM_PLAYLIST -> { /* Not used in home screen */ }
                        MediaOption.DELETE -> idsToDelete = listOf(media.mediaId)
                    }
                }
            )
        }
    }

    //Show edit dialog if user hit edit
    mediaToEdit?.let { media ->
        EditMediaDialog(
            media = media,
            onDismiss = { mediaToEdit = null },
            onConfirm = { updatedMedia ->
                mediaToEdit = null
                viewModel.updateMediaItem(updatedMedia)
            }
        )
    }

    //Show PlaylistPicker if user clicks Add to Playlist (bulk or single)
    if (idsToAddToPlaylists.isNotEmpty()) {
        PlaylistPicker(
            playlists = playlists,
            onDismiss = { idsToAddToPlaylists = emptyList() },
            onConfirm = { selectedPlaylistIds ->
                viewModel.addMediaToPlaylists(idsToAddToPlaylists, selectedPlaylistIds)
                idsToAddToPlaylists = emptyList()
            }
        )
    }

    //Show SortOrderDialog if user clicks Sort button
    if (showSortDialog) {
        SortOrderDialog(
            title = "Sort Media By",
            options = MediaSortOrder.entries.toTypedArray(),
            currentSelection = sortOrder,
            onDismiss = { showSortDialog = false },
            onOptionSelected = { option ->
                showSortDialog = false
                viewModel.onSortOrderChange(option)
            }
        )
    }

    //Show delete confirmation dialog if user hit delete
    if (idsToDelete.isNotEmpty()) { //TODO: Maybe be more descriptive (show title) when deleting 1 item
        ConfirmationDialog(
            title = "Are you sure you want to delete ${if (idsToDelete.size > 1) "these ${idsToDelete.size} items" else "this item"} from your library?",
            text = "This action cannot be undone",
            onDismiss = { idsToDelete = emptyList() },
            onConfirm = {
                viewModel.deleteMediaByIds(idsToDelete)
                idsToDelete = emptyList()
                viewModel.clearSelection()
            }
        )
    }
}

