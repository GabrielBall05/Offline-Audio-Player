package com.example.offlineplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.offlineplayer.ui.components.common.FilterButton
import com.example.offlineplayer.ui.components.common.SearchBar
import com.example.offlineplayer.ui.viewmodels.PlaylistsViewModel

@Composable
fun PlaylistScreen(viewModel: PlaylistsViewModel = hiltViewModel()) { //Let Hilt inject the ViewModel
    //Collect states from ViewModel
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val playlistList by viewModel.filteredPlaylists.collectAsStateWithLifecycle()


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
                onValueChange = { viewModel.onSearchQueryChange(it) }
            )

            //Filter
            FilterButton(onClick = { /* TODO: Open Filter/Sort Dialog for All Playlists List */ })
        }


    }
}