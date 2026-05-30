package com.example.offlineplayer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.offlineplayer.ui.components.common.SurfacedImage
import com.example.offlineplayer.ui.components.dialogs.ConfirmationDialog
import com.example.offlineplayer.ui.components.dialogs.EditMediaBulkDialog
import com.example.offlineplayer.ui.components.dialogs.EditMediaDialog
import com.example.offlineplayer.ui.components.dialogs.MediaPicker
import com.example.offlineplayer.ui.components.dialogs.PlaylistFormDialog
import com.example.offlineplayer.ui.components.dialogs.PlaylistPicker
import com.example.offlineplayer.ui.components.listitems.MediaListItem
import com.example.offlineplayer.ui.components.listitems.MediaListItemReorderable
import com.example.offlineplayer.ui.components.optionsheets.MediaOption
import com.example.offlineplayer.ui.components.optionsheets.MediaOptionsSheetContent
import com.example.offlineplayer.ui.components.optionsheets.PlaylistOption
import com.example.offlineplayer.ui.components.optionsheets.PlaylistOptionsSheet
import com.example.offlineplayer.ui.viewmodels.PlaylistDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    viewModel: PlaylistDetailsViewModel = hiltViewModel(), //Let Hilt inject ViewModel
    onBack: () -> Unit,
    onPlayMediaClick: (MediaEntity) -> Unit,
    onAddToQueueClick: (List<MediaEntity>) -> Unit,
    onPlayPlaylistClick: (Int) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val itemCount by viewModel.itemCount.collectAsStateWithLifecycle()
    val mediaList by viewModel.filteredMedia.collectAsStateWithLifecycle()
    val fullMediaList by viewModel.playlistMedia.collectAsStateWithLifecycle()
    val availablePlaylists by viewModel.availablePlaylists.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isAnySelected by viewModel.isAnySelected.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState()

    var isReordering by rememberSaveable { mutableStateOf(false) }
    var idsToRemove by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var idsToAddToPlaylists by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var selectedMediaItemForMenu by rememberSaveable { mutableStateOf<MediaEntity?>(null) }
    var mediaToEdit by rememberSaveable { mutableStateOf<MediaEntity?>(null) }
    var idsToEdit by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var idsToUpdateArtwork by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var showMediaPicker by rememberSaveable { mutableStateOf(false) }
    var mediaNotInPlaylist by rememberSaveable { mutableStateOf<List<MediaEntity>>(emptyList()) }
    var isFetchingMedia by rememberSaveable { mutableStateOf(false) }
    var showPlaylistOptionsSheet by rememberSaveable { mutableStateOf(false) }
    var editingPlaylist by rememberSaveable { mutableStateOf(false) }
    var showDeletePlaylistConfirmation by rememberSaveable { mutableStateOf(false) }

    //Launcher for picking artwork image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.updateArtworkBulk(it.toString(), idsToUpdateArtwork)
        }
        idsToUpdateArtwork = emptyList()
    }

    //Jump to top of list when list size changes
    LaunchedEffect(mediaList.size) {
        if (mediaList.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    //Fetch media not in playlist when picker is shown
    LaunchedEffect(showMediaPicker) {
        if (showMediaPicker) {
            isFetchingMedia = true
            mediaNotInPlaylist = viewModel.getMediaNotInPlaylist()
            isFetchingMedia = false
        }
    }

    //Refresh available playlists when the selection for playlist addition is set
    LaunchedEffect(idsToAddToPlaylists) {
        if (idsToAddToPlaylists.isNotEmpty()) viewModel.refreshAvailablePlaylists(idsToAddToPlaylists)
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            //Header (Back button, playlist details, menu button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Back Button
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                }

                //Cover Image, Name, Description, Item Count
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //Artwork
                    SurfacedImage(
                        model = playlist?.coverUri,
                        contentDescription = "Cover Image",
                        modifier = Modifier.clickable(onClick = { editingPlaylist = true }),
                        fallbackIcon = Icons.Default.LibraryMusic,
                        sizeInDp = 80.dp
                    )

                    //Playlist Details
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = playlist?.name ?: "Playlist Name", style = MaterialTheme.typography.titleLarge, maxLines = 1)
                        playlist?.description?.let { description ->
                            Text(text = description, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        }
                        Text(text = "$itemCount items", style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    }
                }

                //Menu Button
                IconButton(onClick = { showPlaylistOptionsSheet = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                }
            }

            if (isReordering) {
                //Done Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { isReordering = false }) {
                        Text("Done")
                    }
                }
            } else {
                //Search Bar + Play Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //Search Bar
                    SearchBar(
                        value = searchQuery,
                        placeHolderText = "Search in playlist",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClear = { viewModel.onSearchQueryChange("") },
                        onValueChange = { viewModel.onSearchQueryChange(it) }
                    )

                    //Play Button
                    IconButton(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f),
                        onClick = {
                            playlist?.let { currentPlaylist ->
                                onPlayPlaylistClick(currentPlaylist.playlistId)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Play Playlist",
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                //Bulk Actions
                BulkActionsBar(
                    isAnySelected = isAnySelected,
                    isAllSelected = isAllSelected,
                    onToggleAllClick = { viewModel.toggleSelectAll() },
                    onClearSelectionClick = { viewModel.clearSelection() }
                ) {
                    IconButton(onClick = { idsToEdit = selectedIds.toList() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = {
                        idsToUpdateArtwork = selectedIds.toList()
                        pickImageLauncher.launch("image/*")
                    }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Update Image")
                    }
                    IconButton(onClick = { idsToAddToPlaylists = selectedIds.toList() }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add To Another Playlist")
                    }
                    IconButton(onClick = { onAddToQueueClick(mediaList.filter { it.mediaId in selectedIds }) }) {
                        Icon(Icons.Default.AddToQueue, contentDescription = "Add Selection to Queue")
                    }
                    IconButton(onClick = { idsToRemove = selectedIds.toList() }) {
                        Icon(Icons.Default.PlaylistRemove, contentDescription = "Remove From Playlist")
                    }
                }
            }

            //Media List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (isReordering) {
                    //Use reorderable list item
                    itemsIndexed(
                        items = fullMediaList,
                        key = { _, media -> media.mediaId }
                    ) { index, media ->
                        MediaListItemReorderable(
                            media = media,
                            isFirst = index == 0,
                            isLast = index == fullMediaList.size - 1,
                            onMoveUp = {
                                if (index > 0) {
                                    viewModel.moveMediaItemPosition(media.mediaId, fullMediaList[index - 1].mediaId)
                                }
                            },
                            onMoveDown = {
                                if (index < fullMediaList.size - 1) {
                                    viewModel.moveMediaItemPosition(media.mediaId, fullMediaList[index + 1].mediaId)
                                }
                            }
                        )
                    }
                } else {
                    //Use regular list item
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
        }

        //Add Media To Playlist Button
        FloatingActionButton(
            onClick = { showMediaPicker = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 16.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "Add Media To Playlist") }
    }


    //Show ModalBottomSheet options for this playlist if user clicks the ellipsis at the top right
    if (showPlaylistOptionsSheet) {
        playlist?.let { currentPlaylist ->
            ModalBottomSheet(
                onDismissRequest = { showPlaylistOptionsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                PlaylistOptionsSheet(
                    playlist = currentPlaylist,
                    showReorderOption = true,
                    onOptionClick = { option ->
                        showPlaylistOptionsSheet = false
                        when (option) {
                            PlaylistOption.EDIT -> editingPlaylist = true
                            PlaylistOption.PLAY_NOW -> onPlayPlaylistClick(currentPlaylist.playlistId)
                            PlaylistOption.ADD_TO_QUEUE -> onAddToQueueClick(fullMediaList)
                            PlaylistOption.ADD_MEDIA -> showMediaPicker = true
                            PlaylistOption.REORDER -> isReordering = true
                            PlaylistOption.DELETE -> showDeletePlaylistConfirmation = true
                        }
                    }
                )
            }
        }
    }

    //Show PlaylistFormDialog if user wants to edit this playlist
    if (editingPlaylist) {
        playlist?.let { currentPlaylist ->
            PlaylistFormDialog(
                playlistToEdit = currentPlaylist,
                onDismiss = { editingPlaylist = false },
                onConfirm = { plist ->
                    editingPlaylist = false
                    viewModel.editPlaylist(plist)
                }
            )
        }
    }

    //Show ConfirmationDialog if user wants to delete this playlist
    if (showDeletePlaylistConfirmation) {
        playlist?.let { currentPlaylist ->
            ConfirmationDialog(
                title = "Are you sure you want to delete the playlist \"${currentPlaylist.name}\"?",
                text = "This action cannot be undone",
                onDismiss = { showDeletePlaylistConfirmation = false },
                onConfirm = {
                    onBack()
                    viewModel.deletePlaylist(currentPlaylist)
                }
            )
        }
    }

    //Show ConfirmationDialog if user wants to remove media items from this playlist
    if (idsToRemove.isNotEmpty()) {
        playlist?.let { currentPlaylist ->
            ConfirmationDialog(
                title = "Are you sure you want to remove ${if (idsToRemove.size > 1) "these ${idsToRemove.size} items" else "this item"} from \"${currentPlaylist.name}\"?",
                text = "You can always re-add ${if (idsToRemove.size > 1) "them" else "it"}.",
                confirmText = "Remove",
                onDismiss = { idsToRemove = emptyList() },
                onConfirm = {
                    viewModel.removeMediaFromPlaylist(idsToRemove)
                    idsToRemove = emptyList()
                    viewModel.clearSelection()
                }
            )
        }
    }

    //Show ModalBottomSheet options for a media item if user clicks ellipsis on that item
    selectedMediaItemForMenu?.let { media ->
        ModalBottomSheet(
            onDismissRequest = { selectedMediaItemForMenu = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MediaOptionsSheetContent(
                media = media,
                showRemoveOption = true,
                onOptionClick = { option ->
                    selectedMediaItemForMenu = null
                    when (option) {
                        MediaOption.EDIT -> mediaToEdit = media
                        MediaOption.PLAY_NOW -> onPlayMediaClick(media)
                        MediaOption.ADD_TO_QUEUE -> onAddToQueueClick(listOf(media))
                        MediaOption.ADD_TO_PLAYLIST -> idsToAddToPlaylists = listOf(media.mediaId)
                        MediaOption.REMOVE_FROM_PLAYLIST -> idsToRemove = listOf(media.mediaId)
                        MediaOption.DELETE -> { /* Not used in playlist details screen */ }
                    }
                }
            )
        }
    }

    //Show EditMediaDialog if user wants to edit a media item from here
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

    //Show edit dialog if user hit edit (bulk)
    if (idsToEdit.isNotEmpty()) {
        EditMediaBulkDialog(
            itemCount = idsToEdit.size,
            commonCreator = viewModel.getCommonCreator(idsToEdit),
            onDismiss = { idsToEdit = emptyList() },
            onConfirm = { newCreator ->
                viewModel.updateCreatorBulk(newCreator, idsToEdit)
                idsToEdit = emptyList()
            }
        )
    }

    //Show MediaPicker if user wants to add media to this playlist from here
    if (showMediaPicker && !isFetchingMedia) {
        playlist?.let { currentPlaylist ->
            MediaPicker(
                media = mediaNotInPlaylist,
                onDismiss = { showMediaPicker = false },
                onConfirm = { mediaIds ->
                    showMediaPicker = false
                    viewModel.addMediaToPlaylists(mediaIds, listOf(currentPlaylist.playlistId))
                }
            )
        }
    }

    //Show PlaylistPicker if user wants to add items to another playlist from here
    if (idsToAddToPlaylists.isNotEmpty()) {
        PlaylistPicker(
            playlists = availablePlaylists,
            onDismiss = { idsToAddToPlaylists = emptyList() },
            onConfirm = { playlistIds ->
                viewModel.addMediaToPlaylists(idsToAddToPlaylists, playlistIds)
                idsToAddToPlaylists = emptyList()
            }
        )
    }
}
