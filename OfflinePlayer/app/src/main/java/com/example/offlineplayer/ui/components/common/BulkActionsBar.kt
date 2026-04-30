package com.example.offlineplayer.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BulkActionsBar(
    isAnySelected: Boolean = false,
    isAllSelected: Boolean = false,
    onToggleAllClick: () -> Unit,
    onClearSelectionClick: () -> Unit,
    actionButtons: @Composable (RowScope.() -> Unit)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Absolute.Left
    ) {
        //Toggle All Button
        IconButton(onClick = onToggleAllClick) {
            SelectionIcon(isAllSelected)
        }

        //Bulk Actions
        AnimatedVisibility(visible = isAnySelected) { //Only show if 1 or more items selected
            Row {
                Row(modifier = Modifier.weight(1f)) {
                    actionButtons()
                }
                //Clear selection button
                Row {
                    TextButton(onClick = onClearSelectionClick) {
                        Text("Clear Selection")
                    }
                }
            }
        }
    }
}