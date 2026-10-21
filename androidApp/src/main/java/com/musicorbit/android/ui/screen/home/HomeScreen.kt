package com.musicorbit.android.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicorbit.android.R
import com.musicorbit.android.i18n.LocalStrings
import com.musicorbit.android.ui.components.AlbumArtImage
import com.musicorbit.android.ui.components.LyricsBadge
import com.musicorbit.android.ui.components.MiniPlayer
import com.musicorbit.android.ui.components.OrbitBackground
import com.musicorbit.android.ui.theme.OrbitCyan
import com.musicorbit.android.ui.theme.OrbitViolet
import com.musicorbit.android.ui.theme.headlineFontFamily
import com.musicorbit.android.ui.screen.player.PlayerViewModel
import com.musicorbit.android.ui.util.pressableSpring
import com.musicorbit.domain.model.Song
import com.musicorbit.domain.model.SortOrder

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    onSongClick: (Song) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val strings       = LocalStrings.current
    val songs         by viewModel.songs.collectAsStateWithLifecycle()
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val searchQuery   by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder     by viewModel.sortOrder.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

    // Keep the live playback queue aligned with the header sort order (for "Berurut" mode).
    DisposableEffect(viewModel, playerViewModel) {
        viewModel.onQueueShouldFollowSort = { list -> playerViewModel.resyncQueue(list) }
        onDispose { viewModel.onQueueShouldFollowSort = null }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // ── Brand bar ────────────────────────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Clean, fully circular logo — no square frame or boxed container.
                        Image(
                            painter            = painterResource(R.drawable.app_logo),
                            contentDescription = "Music Orbit",
                            modifier           = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Music Orbit",
                            style = TextStyle(
                                brush = Brush.horizontalGradient(
                                    listOf(OrbitViolet, OrbitCyan)
                                ),
                                fontFamily = headlineFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 22.sp,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(Modifier.weight(1f))
                        Box {
                            IconToggle(Icons.Default.Sort, strings.sort) { showSortMenu = true }
                            DropdownMenu(
                                expanded         = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text      = { Text(order.toLabel(strings)) },
                                        onClick   = {
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
                        IconToggle(Icons.Default.Settings, strings.settings) { onNavigateToSettings() }
                    }

                    // ── Inline header search (always visible, rounded) ────────────
                    OutlinedTextField(
                        value           = searchQuery,
                        onValueChange   = viewModel::onSearchQueryChange,
                        placeholder     = { Text(strings.searchHint) },
                        leadingIcon     = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon    = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = strings.clear)
                                }
                            }
                        },
                        singleLine      = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        shape           = RoundedCornerShape(18.dp),
                        modifier        = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor   = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {

                if (songs.isEmpty()) {
                    Box(
                        modifier         = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(strings.noSongs, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.triggerScan() }) { Text(strings.rescan) }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier      = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top    = innerPadding.calculateTopPadding() + 4.dp,
                            bottom = 148.dp + innerPadding.calculateBottomPadding(),
                            start  = 12.dp,
                            end    = 12.dp
                        )
                    ) {
                        itemsIndexed(songs, key = { _, s -> s.id }) { _, song ->
                            SongRow(
                                song    = song,
                                onClick = {
                                    playerViewModel.playFromQueue(songs, songs.indexOf(song))
                                    onSongClick(song)
                                }
                            )
                        }
                    }
                }

                // Floating MiniPlayer overlay with a clear separator on top.
                AnimatedVisibility(
                    visible = playbackState.currentSong != null,
                    enter   = fadeIn() + slideInVertically { it },
                    exit    = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    playbackState.currentSong?.let { song ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            MiniPlayer(
                                song              = song,
                                isPlaying         = playbackState.isPlaying,
                                onTogglePlayPause = playerViewModel::togglePlayPause,
                                onPrevious        = playerViewModel::skipPrevious,
                                onNext            = playerViewModel::skipNext,
                                onClick           = { onSongClick(song) },
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .pressableSpring(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc)
    }
}

/** Simple, unboxed song row — no card, no entrance animation, just a clean tappable line. */
@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(
            uri      = song.albumArtUri,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = song.title,
                style    = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
    )
}

private fun SortOrder.toLabel(strings: com.musicorbit.android.i18n.AppStrings): String = when (this) {
    SortOrder.DATE_ADDED  -> strings.sortNewest
    SortOrder.PLAY_COUNT  -> strings.sortMostPlayed
    SortOrder.TITLE       -> strings.sortTitleAz
    SortOrder.ARTIST      -> strings.sortArtist
}
