package com.example.offlineplayer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.offlineplayer.data.MediaEntity
import com.example.offlineplayer.ui.components.common.FilterButton
import com.example.offlineplayer.ui.components.common.MenuOptionItem
import com.example.offlineplayer.ui.components.common.SearchBar
import com.example.offlineplayer.ui.components.media.MediaListItem
import com.example.offlineplayer.ui.components.common.SelectionIcon
import com.example.offlineplayer.ui.components.dialogs.DeleteConfirmationDialog
import com.example.offlineplayer.ui.components.dialogs.EditMediaDialog
import com.example.offlineplayer.ui.components.media.MediaOption
import com.example.offlineplayer.ui.components.media.MediaOptionsSheetContent
import com.example.offlineplayer.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) { //Let Hilt inject the ViewModel
    //Collect states from ViewModel
    val mediaList by viewModel.filteredMedia.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isAnySelected by viewModel.isAnySelected.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState()

    var idsToDelete by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var selectedMediaItemForMenu by remember { mutableStateOf<MediaEntity?>(null) }
    var mediaToEdit by remember { mutableStateOf<MediaEntity?>(null) }

    //File Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenMultipleDocuments()) {
        uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMedia(uris)
        }
    }

    LaunchedEffect(mediaList.size) {
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
                    onValueChange = { viewModel.onSearchQueryChange(it) }
                )

                //Filter
                FilterButton(onClick = { /* TODO: Open Sort/Filter Dialog For All Media List */ })
            }

            //Bulk Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Absolute.Left
            ) {
                //Bulk Action - Select All Toggle
                IconButton(onClick = { viewModel.toggleSelectAll() }) {
                    SelectionIcon(isAllSelected)
                }

                //Bulk Actions - Add to Playlist, Delete
                AnimatedVisibility(visible = isAnySelected) { //Only show if 1 or more items selected
                    Row {
                        IconButton(onClick = { /*TODO: Open Playlist Picker*/ }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add To Playlist")
                        }
                        IconButton(onClick = { idsToDelete = selectedIds.toList() }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete")
                        }
                    }
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
                onOptionClick = { option ->
                    selectedMediaItemForMenu = null
                    when (option) {
                        MediaOption.EDIT -> mediaToEdit = media
                        MediaOption.PLAY_NOW -> { viewModel.playMedia(media) }
                        MediaOption.ADD_TO_QUEUE -> { viewModel.addMediaToQueue(media) }
                        MediaOption.ADD_TO_PLAYLIST -> { /* TODO: Open Playlist Picker */ }
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
            onConfirm = { updated ->
                viewModel.updateMediaItem(updated)
                mediaToEdit = null
            }
        )
    }

    //Show delete confirmation dialog if user hit delete
    if (idsToDelete.isNotEmpty()) {
        DeleteConfirmationDialog(
            count = idsToDelete.size,
            onDismiss = { idsToDelete = emptyList() },
            onConfirm = {
                viewModel.deleteMediaByIds(idsToDelete)
                idsToDelete = emptyList()
            }
        )
    }
}

