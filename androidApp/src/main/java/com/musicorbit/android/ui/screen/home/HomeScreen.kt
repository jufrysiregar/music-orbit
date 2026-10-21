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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
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

    // Inline, expandable header search: the search field lives inside the SAME header row and
    // only appears when activated, hiding the Sort/Settings icons while open.
    var searchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(searchActive) {
        if (searchActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    fun closeSearch() {
        viewModel.onSearchQueryChange("")
        searchActive = false
        keyboardController?.hide()
    }

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
                    // ── Brand bar (logo + title always visible; search expands inline) ───
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter            = painterResource(R.drawable.app_logo),
                            contentDescription = "Music Orbit",
                            modifier           = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text     = "Music Orbit",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                brush = Brush.horizontalGradient(
                                    listOf(OrbitViolet, OrbitCyan)
                                ),
                                fontFamily = headlineFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 20.sp,
                                letterSpacing = (-0.5).sp
                            )
                        )

                        if (!searchActive) {
                            // Default state: 3 action icons on the right (Search, Sort, Settings).
                            Spacer(Modifier.weight(1f))
                            IconToggle(Icons.Default.Search, strings.search) { searchActive = true }
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
                        } else {
                            // Active state: search field expands within the same header row.
                            Spacer(Modifier.width(12.dp))
                            HeaderSearchField(
                                query          = searchQuery,
                                onQueryChange  = viewModel::onSearchQueryChange,
                                onClose        = ::closeSearch,
                                focusRequester = focusRequester,
                                hint           = strings.searchHint,
                                modifier       = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {

                // Song list only. There is intentionally NO "no music / rescan" screen: the
                // permission + initial scan already ran, the list is a reactive DB flow, and a
                // silent re-scan fires on every resume — so newly added songs appear automatically.
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

/**
 * Single rounded search container shown inline in the header: [ icon | divider | field | divider | X ].
 */
@Composable
private fun HeaderSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester,
    hint: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier     = modifier.height(44.dp),
        shape        = RoundedCornerShape(24.dp),
        color        = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            VerticalDivider(Modifier.padding(horizontal = 8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text  = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value       = query,
                    onValueChange = onQueryChange,
                    singleLine  = true,
                    textStyle   = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier    = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            VerticalDivider(Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
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
