package com.example.offlineplayer.ui.components.dialogs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.offlineplayer.data.local.PlaylistEntity

@Composable
fun PlaylistFormDialog(
    playlistToEdit: PlaylistEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (PlaylistEntity) -> Unit
) {
    var name by remember { mutableStateOf(playlistToEdit?.name ?: "") }
    var description by remember { mutableStateOf(playlistToEdit?.description ?: "") }
    var coverUri by remember { mutableStateOf(playlistToEdit?.coverImage ?: "") }

    var nameTouched by remember { mutableStateOf(false) }

    //Launcher for picking cover image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { coverUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(playlistToEdit?.let { "Editing Playlist: \"${playlistToEdit.name}\"" } ?: "Create Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                //Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameTouched = true
                    },
                    label = { Text("Name *") },
                    isError = nameTouched && name.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                //Description input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                //Cover Image
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.LightGray
                    ) {
                        if (coverUri != null) { //TODO: Change to actual artwork using Coil and AsyncImage
                            Icon(Icons.Default.Image, contentDescription = "Cover Image", modifier = Modifier.padding(16.dp))
                        } else {
                            Icon(Icons.Default.MusicNote, contentDescription = "Cover Image", modifier = Modifier.padding(16.dp))
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    TextButton(onClick = { pickImageLauncher.launch("image/*") }) {
                        Text("Select Cover Image")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDescription = description.takeIf { it.isNotBlank() }
                    val finalCoverUri = coverUri.takeIf { it.isNotBlank() }

                    playlistToEdit?.let {
                        onConfirm(it.copy(name = name, description = finalDescription, coverImage = finalCoverUri))
                    } ?: run {
                        val newPlaylist = PlaylistEntity(playlistId = 0, name = name, description = finalDescription, coverImage = finalCoverUri, dateCreated = System.currentTimeMillis())
                        onConfirm(newPlaylist)
                    }
                },
                enabled = name.isNotBlank()
            ) { Text(playlistToEdit?.let { "Save" } ?: "Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}