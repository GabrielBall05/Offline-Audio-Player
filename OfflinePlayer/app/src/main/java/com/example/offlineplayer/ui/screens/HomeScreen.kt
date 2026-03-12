package com.example.offlineplayer.ui.screens

import android.net.Uri
import android.widget.CheckBox
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.offlineplayer.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel() //Let Hilt inject the ViewModel
) {
    val context = LocalContext.current

    //Collect states from ViewModel
    val mediaList by viewModel.allMedia.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedIds by viewModel.selectedMediaIds.collectAsState()
    val isAnySelected by viewModel.isAnySelected.collectAsState()

    val isAllSelected = mediaList.isNotEmpty() && (selectedIds.size == mediaList.size)
    var showDeleteDialog by remember { mutableStateOf(false) }

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
            IconButton(onClick = { /*TO DO - Open Sort Filter Menu*/ }) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter List")
            }
        }

        //Bulk Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(215, 210, 190))
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
                    IconButton(onClick = { /*ADD TO PLAYLIST*/ }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add To Playlist")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
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
            items(mediaList) { media ->
                MediaListItem(
                    title = media.title,
                    creator = media.creator,
                    isSelected = selectedIds.contains(media.mediaId),
                    onCheckBoxClick = { viewModel.toggleSelection(media.mediaId) },
                    onMoreClick = { /*To Do - Show Options Menu (maybe in separate function)*/ }
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
            //Add Media Button
            Button(onClick = { filePickerLauncher.launch(arrayOf("audio/*")) }) { //Filters for audio files only
                Icon(Icons.Default.Add, contentDescription = "Add Media")
            }
        }
    }

    //Show delete confirmation dialog if user hit delete
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Media") },
            text = { Text("Are you sure you want to remove ${selectedIds.size} item${if (selectedIds.size != 1) "s" else ""}" +
                    " from your library? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMedia()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaListItem(
    title: String,
    creator: String,
    isSelected: Boolean,
    onCheckBoxClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 2.dp,
                bottom = 2.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Selection Checkbox
        IconButton(onClick = onCheckBoxClick) {
            SelectionIcon(isSelected)
        }

        //Artwork Placeholder
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.LightGray
        ) {
            //PLACEHOLDER IMAGE
            Icon(Icons.Default.MusicNote, contentDescription = "Artwork Image")
        }

        //Media Item Info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            //Title
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
            //Creator
            Text(
                text = creator,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
        }

        //More Button (ellipsis) - brings up menu for edit, play, add to playlist, delete, etc.
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.Gray)
        }
    }
}

@Composable
fun SelectionIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = if(isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
        contentDescription = if (isSelected) "Select Item" else "Deselect Item",
        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
        modifier = modifier
    )
}











