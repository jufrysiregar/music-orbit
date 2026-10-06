package com.musicorbit.android.ui.screen.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToLyrics: (Long) -> Unit
) {
    val state          by viewModel.playbackState.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }

    // System back → go back to home
    BackHandler { onNavigateToHome() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Daftar Lagu"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ── Album Art ────────────────────────────────────────────────────
            AlbumArtImage(
                uri      = state.currentSong?.albumArtUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
            )

            // ── Song info block ──────────────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Title
                Text(
                    text     = state.currentSong?.title ?: "Tidak ada lagu",
                    style    = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))

                // Artist + Edit button on same row
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = state.currentSong?.artist ?: "–",
                        style    = MaterialTheme.typography.bodyLarge,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick  = { showEditDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = "Edit metadata",
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── SeekBar ──────────────────────────────────────────────────────
            SeekBarSection(
                position = state.currentPosition,
                duration = state.duration,
                onSeek   = viewModel::seekTo
            )

            // ── Main playback controls ────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick  = viewModel::skipPrevious,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Sebelumnya",
                        modifier           = Modifier.size(40.dp)
                    )
                }

                FilledIconButton(
                    onClick  = viewModel::togglePlayPause,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector        = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Jeda" else "Putar",
                        modifier           = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick  = viewModel::skipNext,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Berikutnya",
                        modifier           = Modifier.size(40.dp)
                    )
                }
            }

            // ── Secondary controls: EQ | Lyrics | Play Mode ──────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToEqualizer) {
                    Icon(Icons.Default.Tune, contentDescription = "Equalizer")
                }

                FilledTonalButton(
                    onClick = {
                        state.currentSong?.id?.let { onNavigateToLyrics(it) }
                    }
                ) {
                    Icon(
                        Icons.Default.Lyrics,
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Lirik")
                }

                IconButton(onClick = viewModel::cyclePlayMode) {
                    Icon(
                        imageVector        = when (state.playMode) {
                            PlayMode.REPEAT_ONE -> Icons.Default.RepeatOne
                            PlayMode.SHUFFLE    -> Icons.Default.Shuffle
                            PlayMode.SEQUENTIAL -> Icons.Default.Repeat
                        },
                        contentDescription = "Mode: ${state.playMode.name}"
                    )
                }
            }
        }
    }

    // ── Edit metadata dialog ──────────────────────────────────────────────────
    if (showEditDialog && state.currentSong != null) {
        EditSongMetadataDialog(
            song      = state.currentSong!!,
            onSave    = { updated ->
                viewModel.updateSongMetadata(updated)
                showEditDialog = false
            },
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
                    value         = title,
                    onValueChange = { title = it },
                    label         = { Text("Judul Lagu") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = artist,
                    onValueChange = { artist = it },
                    label         = { Text("Artis") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    onSave(song.copy(title = title.trim(), artist = artist.trim()))
                }
            }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
