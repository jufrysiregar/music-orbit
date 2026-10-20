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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.musicorbit.android.ui.components.AlbumArtImage
import com.musicorbit.android.ui.components.GlassCard
import com.musicorbit.android.ui.components.LyricsBadge
import com.musicorbit.android.ui.components.MiniPlayer
import com.musicorbit.android.ui.components.OrbitBackground
import com.musicorbit.android.ui.theme.OrbitCyan
import com.musicorbit.android.ui.theme.OrbitViolet
import com.musicorbit.android.ui.theme.OrbitMagenta
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
    val songs         by viewModel.songs.collectAsStateWithLifecycle()
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val searchQuery   by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder     by viewModel.sortOrder.collectAsStateWithLifecycle()

    var showSearch   by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val focusRequester   = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // ── Brand bar ────────────────────────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(auroraOverlay()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.app_logo),
                                contentDescription = "Music Orbit",
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp))
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
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
                            Text(
                                text  = "Musikmu, orbitmu",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconToggle(Icons.Default.Search, "Cari") { showSearch = true }
                        Box {
                            IconToggle(Icons.Default.Sort, "Urutkan") { showSortMenu = true }
                            DropdownMenu(
                                expanded         = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text      = { Text(order.toLabel()) },
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
                        IconToggle(Icons.Default.Settings, "Pengaturan") { onNavigateToSettings() }
                    }

                    // ── Expanding search ─────────────────────────────────────
                    AnimatedVisibility(
                        visible = showSearch,
                        enter   = expandVertically(tween(220)) + fadeIn(),
                        exit    = shrinkVertically(tween(180)) + fadeOut()
                    ) {
                        Row(
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value           = searchQuery,
                                onValueChange   = viewModel::onSearchQueryChange,
                                placeholder     = { Text("Cari lagu atau artis...") },
                                leadingIcon     = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon    = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                        }
                                    }
                                },
                                singleLine      = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                shape           = RoundedCornerShape(18.dp),
                                modifier        = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                colors          = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor    = OrbitViolet,
                                    unfocusedBorderColor  = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {
                                showSearch = false
                                viewModel.onSearchQueryChange("")
                                keyboardController?.hide()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup pencarian")
                            }
                        }
                    }
                    LaunchedEffect(showSearch) {
                        if (showSearch) {
                            kotlinx.coroutines.delay(120)
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {

                if (songs.isEmpty()) {
                    Box(
                        modifier         = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(modifier = Modifier.padding(24.dp), glow = true) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier            = Modifier.padding(8.dp)
                            ) {
                                Text("Tidak ada musik ditemukan", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { viewModel.triggerScan() }) { Text("Pindai Ulang") }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier      = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top    = innerPadding.calculateTopPadding() + 8.dp,
                            bottom = 100.dp + innerPadding.calculateBottomPadding(),
                            start  = 12.dp,
                            end    = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                            SongRow(
                                song    = song,
                                stagger = index.coerceAtMost(10),
                                onClick = {
                                    playerViewModel.playFromQueue(songs, songs.indexOf(song))
                                    onSongClick(song)
                                }
                            )
                        }
                    }
                }

                // Floating MiniPlayer overlay
                AnimatedVisibility(
                    visible = playbackState.currentSong != null,
                    enter   = fadeIn() + slideInVertically { it },
                    exit    = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    playbackState.currentSong?.let { song ->
                        MiniPlayer(
                            song              = song,
                            isPlaying         = playbackState.isPlaying,
                            onTogglePlayPause = playerViewModel::togglePlayPause,
                            onNext            = playerViewModel::skipNext,
                            onClick           = { onSongClick(song) },
                            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
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

@Composable
private fun SongRow(song: Song, stagger: Int, onClick: () -> Unit) {
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(stagger * 45L)
        appear = true
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (appear) 1f else 0f
                translationY = if (appear) 0f else 40f
            }
            .pressableSpring(onClick = onClick),
        shape    = RoundedCornerShape(20.dp),
        contentPadding = 8.dp
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtImage(
                uri      = song.albumArtUri,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = song.title,
                    style    = MaterialTheme.typography.titleMedium,
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
    }
}

private fun auroraOverlay() = Brush.linearGradient(
    listOf(OrbitViolet.copy(alpha = 0.35f), OrbitMagenta.copy(alpha = 0.25f))
)

private fun SortOrder.toLabel(): String = when (this) {
    SortOrder.DATE_ADDED  -> "Terbaru"
    SortOrder.PLAY_COUNT  -> "Paling sering diputar"
    SortOrder.TITLE       -> "A - Z"
    SortOrder.ARTIST      -> "Artis"
}
