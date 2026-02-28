package com.example.offlineplayer.ui.screens

import android.widget.CheckBox
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    //TEMPORARY - Will be moved to ViewModel
    var searchQuery by remember { mutableStateOf("") }

    //Tracks IDs of selected media items
    val selectedMediaIds = remember { mutableStateListOf<Int>() }
    val isAnySelected = selectedMediaIds.isNotEmpty()

    //TEMPORARY count '20'
    val totalItems = 20
    val isAllSelected = selectedMediaIds.size == totalItems && totalItems > 0

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
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
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
            IconButton(onClick = { //Move to separate function for clean code
                if (isAllSelected)
                    selectedMediaIds.clear()
                else {
                    selectedMediaIds.clear()
                    repeat (totalItems) {
                        selectedMediaIds.add(it)
                    }
                }
            }) {
                Icon(
                    imageVector = if(isAllSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = if (isAllSelected) "Select All" else "Deselect All",
                    tint = if (isAllSelected) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            //Bulk Actions - Add to Playlist, Delete
            AnimatedVisibility(visible = isAnySelected) { //Only show if > 0 items selected
                Row {
                    IconButton(onClick = { /*ADD TO PLAYLIST*/ }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add To Playlist")
                    }
                    IconButton(onClick = { /*DELETE*/ }) {
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
            //To Do - Replace 'items(20)' with real data from Room
            items(20) { index ->
                val isSelected = selectedMediaIds.contains(index)

                MediaListItem(
                    title = "Media Title $index", //Temporary hardcoded strings
                    creator = "Creator Name",
                    isSelected = isSelected,
                    onCheckBoxClick = {
                        if (isSelected) selectedMediaIds.remove(index)
                        else selectedMediaIds.add(index)
                    },
                    onMoreClick = { /*To Do - Show Options Menu*/ }
                )
            }
        }

        //Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            //Action Button - Add Media
            Button(onClick = { /*To Do - Open File Picker*/ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Media")
            }
        }
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
            Icon(
                imageVector = if(isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (isSelected) "Select Item" else "Deselect Item",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
            )
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
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(creator, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }

        //More Button (ellipses) - brings up menu for edit, play, add to playlist, delete, etc.
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "Edit", tint = Color.Gray)
        }
    }
}













