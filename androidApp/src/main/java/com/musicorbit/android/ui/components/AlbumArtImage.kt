package com.musicorbit.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicorbit.android.R

@Composable
fun AlbumArtImage(
    uri: String?,
    modifier: Modifier = Modifier
) {
    if (uri.isNullOrBlank()) {
        // Placeholder: use app logo
        Box(
            modifier          = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment  = Alignment.Center
        ) {
            Image(
                painter            = painterResource(R.drawable.app_logo),
                contentDescription = "Album art tidak tersedia",
                modifier           = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale       = ContentScale.Fit
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(false)
                .build(),
            contentDescription = "Album Art",
            contentScale       = ContentScale.Crop,
            modifier           = modifier
        )
    }
}
