package com.musicorbit.android.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicorbit.android.ui.components.AlbumArtImage
import com.musicorbit.android.ui.components.LyricsBadge
import com.musicorbit.android.ui.components.MiniPlayer
import com.musicorbit.android.ui.screen.player.PlayerViewModel
import com.musicorbit.domain.model.Song
import com.musicorbit.domain.model.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    onSongClick: (Song) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val songs         by viewModel.songs.collectAsStateWithLifecycle()
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val searchQuery   by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder     by viewModel.sortOrder.collectAsStateWithLifecycle()

    var showSearch    by remember { mutableStateOf(false) }
    var showSortMenu  by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value         = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder   = { Text("Cari lagu atau artis...") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // App icon placeholder (actual icon configured in Phase 6 Task 22)
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Music Orbit", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                },
                actions = {
                    // Search
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) viewModel.onSearchQueryChange("")
                    }) {
                        Icon(
                            imageVector        = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Cari"
                        )
                    }

                    // Sort dropdown
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Urutkan")
                        }
                        DropdownMenu(
                            expanded        = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.toLabel()) },
                                    onClick = {
                                        viewModel.onSortOrderChange(order)
                                        showSortMenu = false
                                    },
                                    leadingIcon = if (order == sortOrder) ({
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }) else null
                                )
                            }
                        }
                    }

                    // Settings
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {

            if (songs.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = "Tidak ada musik ditemukan di perangkat ini",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.triggerScan() }) {
                            Text("Pindai Ulang")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top    = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = 88.dp + innerPadding.calculateBottomPadding()
                    )
                ) {
                    items(songs, key = { it.id }) { song ->
                        SongListItem(
                            song    = song,
                            onClick = {
                                playerViewModel.playSong(song)
                                onSongClick(song)
                            }
                        )
                    }
                }
            }

            // Floating MiniPlayer overlay
            if (playbackState.currentSong != null) {
                MiniPlayer(
                    song              = playbackState.currentSong!!,
                    isPlaying         = playbackState.isPlaying,
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onNext            = playerViewModel::skipNext,
                    onClick           = { onSongClick(playbackState.currentSong!!) },
                    modifier          = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun SongListItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(
            uri      = song.albumArtUri,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = song.title,
                style    = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.hasLyrics) {
                    LyricsBadge()
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text     = song.artist,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 84.dp))
}

private fun SortOrder.toLabel(): String = when (this) {
    SortOrder.DATE_ADDED  -> "Terbaru"
    SortOrder.PLAY_COUNT  -> "Paling sering diputar"
    SortOrder.TITLE       -> "A - Z"
    SortOrder.ARTIST      -> "Artis"
}
