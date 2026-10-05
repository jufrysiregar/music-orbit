package com.musicorbit.android.ui.screen.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicorbit.android.ui.components.AlbumArtImage
import com.musicorbit.android.ui.components.SeekBarSection
import com.musicorbit.domain.model.PlayMode
import com.musicorbit.domain.model.Song

@Composable
fun MainPlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToLyrics: (Long) -> Unit
) {
    val state by viewModel.playbackState.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }

    BackHandler { onNavigateToHome() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Album Art ───────────────────────────────────────────────────────
        AlbumArtImage(
            uri = state.currentSong?.albumArtUri,
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        // ── Title ───────────────────────────────────────────────────────────
        Text(
            text      = state.currentSong?.title ?: "Tidak ada lagu",
            style     = MaterialTheme.typography.headlineMedium,
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // ── Artist row + Edit icon ──────────────────────────────────────────
        Row(
            modifier       = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text     = state.currentSong?.artist ?: "--",
                style    = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showEditDialog = true }) {
                Icon(
                    imageVector        = Icons.Default.Edit,
                    contentDescription = "Edit metadata lagu"
                )
            }
        }

        // ── SeekBar + time ──────────────────────────────────────────────────
        SeekBarSection(
            position = state.currentPosition,
            duration = state.duration,
            onSeek   = viewModel::seekTo
        )

        // ── Main controls ────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::skipPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya", modifier = Modifier.size(36.dp))
            }
            FilledIconButton(
                onClick  = viewModel::togglePlayPause,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector        = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Jeda" else "Putar",
                    modifier           = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = viewModel::skipNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya", modifier = Modifier.size(36.dp))
            }
        }

        // ── Bottom row: EQ | Lyrics | PlayMode ────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToEqualizer) {
                Icon(Icons.Default.Tune, contentDescription = "Equalizer")
            }
            TextButton(onClick = { state.currentSong?.id?.let { onNavigateToLyrics(it) } }) {
                Icon(Icons.Default.Lyrics, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Lirik")
            }
            IconButton(onClick = viewModel::cyclePlayMode) {
                Icon(
                    imageVector = when (state.playMode) {
                        PlayMode.REPEAT_ONE -> Icons.Default.RepeatOne
                        PlayMode.SHUFFLE    -> Icons.Default.Shuffle
                        PlayMode.SEQUENTIAL -> Icons.Default.RepeatOn
                    },
                    contentDescription = "Mode pemutaran: ${state.playMode.name}"
                )
            }
        }
    }

    // ── Edit Metadata Dialog ─────────────────────────────────────────────────
    if (showEditDialog && state.currentSong != null) {
        EditSongMetadataDialog(
            song    = state.currentSong!!,
            onSave  = { updated -> viewModel.updateSongMetadata(updated); showEditDialog = false },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
private fun EditSongMetadataDialog(
    song: Song,
    onSave: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    var title  by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Edit Informasi Lagu") },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Lagu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artis") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(song.copy(title = title.trim(), artist = artist.trim()))
            }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
