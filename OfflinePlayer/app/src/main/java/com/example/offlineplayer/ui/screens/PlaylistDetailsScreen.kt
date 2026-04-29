package com.example.offlineplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.offlineplayer.ui.components.common.SearchBar
import com.example.offlineplayer.ui.theme.OfflinePlayerTheme
import com.example.offlineplayer.ui.viewmodels.PlaylistDetailsViewModel

@Composable
fun PlaylistDetailsScreen(
    onBack: () -> Unit,
    viewModel: PlaylistDetailsViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val mediaList by viewModel.filteredMedia.collectAsStateWithLifecycle()

    var addingMedia by remember { mutableStateOf(false) }

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
                    text = playlist?.name ?: "Playlist Details",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Search
                SearchBar(
                    value = searchQuery,
                    placeHolderText = "Search in playlist",
                    modifier = Modifier.fillMaxWidth(),
                    onClear = { viewModel.onSearchQueryChange("") },
                    onValueChange = { viewModel.onSearchQueryChange(it) }
                )
            }
        }

        //Add To Playlist Button
        FloatingActionButton(
            onClick = { addingMedia = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add To Playlist")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PlaylistDetailsScreenPreview() {
    OfflinePlayerTheme {
        PlaylistDetailsScreen(
            onBack = {}
        )
    }
}