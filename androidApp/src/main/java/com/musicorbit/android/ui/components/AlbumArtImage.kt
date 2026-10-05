package com.musicorbit.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun AlbumArtImage(
    uri: String?,
    modifier: Modifier = Modifier
) {
    if (uri.isNullOrBlank()) {
        // Placeholder
        Box(
            modifier          = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment  = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.MusicNote,
                contentDescription = "Album art tidak tersedia",
                modifier           = Modifier.size(48.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = "Album Art",
            contentScale       = ContentScale.Crop,
            modifier           = modifier
        )
    }
}
