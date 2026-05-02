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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.offlineplayer.data.local.MediaEntity

@Composable
fun EditMediaDialog(
    media: MediaEntity,
    onDismiss: () -> Unit,
    onConfirm: (MediaEntity) -> Unit
) {
    var title by remember { mutableStateOf(media.title) }
    var creator by remember { mutableStateOf(media.creator) }
    var artworkUri by remember { mutableStateOf(media.artworkUri) }

    var titleTouched by remember { mutableStateOf(false) }
    var creatorTouched by remember { mutableStateOf(false) }

    //Launcher for picking artwork image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { artworkUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                //Edit title input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleTouched = true
                    },
                    isError = titleTouched && title.isBlank(),
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                //Edit creator input
                OutlinedTextField(
                    value = creator,
                    onValueChange = {
                        creator = it
                        creatorTouched = true
                    },
                    isError = creatorTouched && creator.isBlank(),
                    label = { Text("Creator *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                //Artwork Image
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