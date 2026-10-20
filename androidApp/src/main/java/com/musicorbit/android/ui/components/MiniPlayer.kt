package com.musicorbit.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicorbit.android.i18n.LocalStrings
import com.musicorbit.android.ui.navigation.OrbitKeys
import com.musicorbit.android.ui.navigation.orbitSharedElement
import com.musicorbit.android.ui.theme.OrbitViolet
import com.musicorbit.android.ui.util.pressableSpring
import com.musicorbit.domain.model.Song

/**
 * Compact player bar pinned to the bottom of HomeScreen.
 * Only the artwork + text open the full player; the transport buttons have their own
 * independent tap targets so play / previous / next always work.
 */
@androidx.compose.animation.ExperimentalSharedTransitionApi
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Row(
        modifier          = modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tapping this region opens the full player.
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtImage(
                uri      = song.albumArtUri,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .orbitSharedElement(OrbitKeys.AlbumArt)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = song.title,
                    style    = MaterialTheme.typography.titleSmall,
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
        }

        // Previous
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .pressableSpring(onClick = onPrevious),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SkipPrevious, contentDescription = strings.previous)
        }

        Spacer(Modifier.width(2.dp))

        // Play / Pause (accent)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .pressableSpring(pressedScale = 0.9f, onClick = onTogglePlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) strings.pause else strings.play,
                tint               = OrbitViolet
            )
        }

        Spacer(Modifier.width(2.dp))

        // Next
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .pressableSpring(onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = strings.next)
        }
    }
}
