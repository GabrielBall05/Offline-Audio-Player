package com.example.offlineplayer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.offlineplayer.data.MediaEntity
import com.example.offlineplayer.ui.components.common.MenuOptionItem
import com.example.offlineplayer.ui.components.media.MediaListItem
import com.example.offlineplayer.ui.components.common.SelectionIcon
import com.example.offlineplayer.ui.components.dialogs.DeleteConfirmationDialog
import com.example.offlineplayer.ui.components.dialogs.EditMediaDialog
import com.example.offlineplayer.ui.components.media.MediaOption
import com.example.offlineplayer.ui.components.media.MediaOptionsSheetContent
import com.example.offlineplayer.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel() //Let Hilt inject the ViewModel
) {
    val context = LocalContext.current

    //Collect states from ViewModel
    val mediaList by viewModel.filteredMedia.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isAnySelected by viewModel.isAnySelected.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle()

    var idsToDelete by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState()
    var selectedMediaItemForMenu by remember { mutableStateOf<MediaEntity?>(null) }
    var mediaToEdit by remember { mutableStateOf<MediaEntity?>(null) }

    //File Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenMultipleDocuments()) {
        uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMedia(uris)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        //Page Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 16.dp,
                    bottom = 6.dp
                ),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Add or Edit Media",
                fontSize = 20.sp
            )
        }

        //Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search media...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            //Filter
            IconButton(onClick = { /*TODO: Open Sort/Filter Menu*/ }) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter List")
            }
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
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = 6.dp,
                    bottom = 0.dp,
                    start = 0.dp,
                    end = 0.dp
                )
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

        //Other Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            //Upload Media Button
            Button(onClick = { filePickerLauncher.launch(arrayOf("audio/*")) }) { //Filters for audio files only
                Icon(Icons.Default.Add, contentDescription = "Upload Media")
                Spacer(Modifier.width(12.dp))
                Text("Upload Media")
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.Add, contentDescription = "Upload Media")
            }
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

