package com.example.offlineplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun PlaylistDetailsScreen(
    playlistId: Int,
    onBack: () -> Unit
) {
    Column() {
        Row(horizontalArrangement = Arrangement.Center) {
            Text("playlist details screen for id: $playlistId")
        }
        Row(horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
            }
        }
    }
}