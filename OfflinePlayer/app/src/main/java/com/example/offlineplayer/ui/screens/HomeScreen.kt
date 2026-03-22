package com.example.offlineplayer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
            //Add Media Button
            Button(onClick = { filePickerLauncher.launch(arrayOf("audio/*")) }) { //Filters for audio files only
                Icon(Icons.Default.Add, contentDescription = "Add Media")
            }
        }
    }

    //Show More menu if user hits ellipsis on a media item
    if (selectedMediaItemForMenu != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedMediaItemForMenu = null },
            sheetState = sheetState
        ) {
            MediaOptionsSheetContent(
                media = selectedMediaItemForMenu!!,
                onOptionClick = { option ->
                    val media = selectedMediaItemForMenu!!
                    selectedMediaItemForMenu = null
                    when (option) {
                        "EDIT" -> { mediaToEdit = media }
                        "ADD_TO_PLAYLIST" -> { /*TODO: Open Playlist Selector*/ }
                        "PLAY" -> { /*TODO: Connect to MediaController*/ }
                        "DELETE" -> { idsToDelete = listOf(media.mediaId) }
                    }
                }
            )
        }
    }

    if (mediaToEdit != null) {
        EditMediaDialog(
            media = mediaToEdit!!,
            onDismiss = { mediaToEdit = null },
            onConfirm = { updatedMedia ->
                viewModel.updateMediaItem(updatedMedia)
                mediaToEdit = null
            }
        )
    }

    //Show delete confirmation dialog if user hit delete
    if (idsToDelete.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { idsToDelete = emptyList() },
            title = { Text("Delete Media") },
            text = { Text("Are you sure you want to delete ${if (idsToDelete.size > 1) " these ${idsToDelete.size} items" else "this item"} from your library? This action cannot be undone.")},
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMediaByIds(idsToDelete)
                        idsToDelete = emptyList()
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { idsToDelete = emptyList() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaListItem(
    media: MediaEntity,
    isSelected: Boolean,
    onCheckBoxClick: () -> Unit,
    onMoreClick: (MediaEntity) -> Unit
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
                text = media.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
            //Creator
            Text(
                text = media.creator,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
        }

        //More Button (ellipsis) - brings up menu for edit, play, add to playlist, delete, etc.
        IconButton(onClick = { onMoreClick(media) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.Gray)
        }
    }
}

@Composable
fun MediaOptionsSheetContent(
    media: MediaEntity,
    onOptionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        //Header
        Text(
            text = media.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
            maxLines = 1
        )

        HorizontalDivider()

        //Menu Options
        //Edit
        MenuOptionItem(Icons.Default.Edit, "Edit Details") { onOptionClick("EDIT") }
        //Add to playlist
        MenuOptionItem(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to Playlist") { onOptionClick("ADD_TO_PLAYLIST") }
        //Play
        MenuOptionItem(Icons.Default.PlayArrow, "Play") { onOptionClick("PLAY") }
        //Delete
        MenuOptionItem(Icons.Default.DeleteForever, "Delete", isDestructive = true) { onOptionClick("DELETE") }
    }
}

@Composable
fun MenuOptionItem(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(label, color = if (isDestructive) Color.Red else Color.Unspecified)
        },
        leadingContent = {
            Icon(icon, contentDescription = label, tint = if (isDestructive) Color.Red else Color.Gray)
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun SelectionIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
        contentDescription = if (isSelected) "Select Item" else "Deselect Item",
        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
        modifier = modifier
    )
}

@Composable
fun EditMediaDialog(
    media: MediaEntity,
    onDismiss: () -> Unit,
    onConfirm: (MediaEntity) -> Unit
) {
    var title by remember { mutableStateOf(media.title) }
    var creator by remember { mutableStateOf(media.creator) }
    var artworkUri by remember { mutableStateOf(media.artworkUri) }

    //Launcher for picking cover image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { artworkUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                //Edit title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                //Edit creator input
                OutlinedTextField(
                    value = creator,
                    onValueChange = { creator = it },
                    label = { Text("Creator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.LightGray
                    ) {
                        if (artworkUri != null) { //TODO: Change to actual artwork using Coil and AsyncImage
                            Icon(Icons.Default.Image, contentDescription = "Artwork Image", modifier = Modifier.padding(16.dp))
                        } else {
                            Icon(Icons.Default.MusicNote, contentDescription = "Artwork Image", modifier = Modifier.padding(16.dp))
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    TextButton(onClick = { pickImageLauncher.launch("image/*") }) {
                        Text("Change Artwork")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(media.copy(
                        title = title,
                        creator = creator,
                        artworkUri = artworkUri
                    ))
                },
                enabled = title.isNotBlank() && creator.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}




