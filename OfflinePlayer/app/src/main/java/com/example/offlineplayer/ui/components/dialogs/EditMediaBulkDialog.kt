package com.example.offlineplayer.ui.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun EditMediaBulkDialog(
    itemCount: Int,
    commonCreator: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var creator by remember { mutableStateOf(commonCreator) }
    var creatorTouched by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editing $itemCount items") },
        text = {
            //Creator
            OutlinedTextField(
                value = creator,
                onValueChange = {
                    creator = it
                    creatorTouched = true
                },
                isError = creator.isBlank() && creatorTouched,
                label = { Text("Creator *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(creator) },
                enabled = creator.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}