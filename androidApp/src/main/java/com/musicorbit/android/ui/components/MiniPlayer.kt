package com.musicorbit.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicorbit.domain.model.Song

/**
 * Compact player bar shown at the bottom of HomeScreen.
 * Tapping anywhere on the bar opens the full player.
 * [onTogglePlayPause] toggles between play and pause.
 * [onNext] skips to the next track.
 */
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier          = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small album art
            AlbumArtImage(
                uri      = song.albumArtUri,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )

            Spacer(Modifier.width(10.dp))

            // Title + artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = song.title,
                    style    = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text     = song.artist,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play / Pause toggle
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector        = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Jeda" else "Putar"
                )
            }

            // Skip next
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya")
            }
        }
    }
}
