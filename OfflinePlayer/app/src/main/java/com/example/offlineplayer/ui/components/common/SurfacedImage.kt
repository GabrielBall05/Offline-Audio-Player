package com.example.offlineplayer.ui.components.common

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

@Composable
fun SurfacedImage(
    model: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fallbackIcon: ImageVector = Icons.Default.MusicNote,
    fallbackIconTint: Color = MaterialTheme.colorScheme.surfaceTint,
    surfaceColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    sizeInDp: Dp = 40.dp
) {
    Log.d("OfflineAudioSuite", "uri: $model")
    Surface(
        modifier = modifier.size(sizeInDp),
        shape = RoundedCornerShape((sizeInDp / 10.dp).dp),
        color = surfaceColor
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop, //TODO: Change ContentScale to something else if it crops badly
            modifier = Modifier.fillMaxSize(),
            error = {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = "Default $contentDescription",
                    tint = fallbackIconTint //TODO: Change to something else or remove entirely
                )
            },
            loading = { /* Leaving blank to let Surface act as the loading placeholder TODO: Maybe change to the same image as error (default fallback) */ }
        )
    }
}