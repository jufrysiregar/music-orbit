package com.musicorbit.android.ui.screen.player

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.musicorbit.android.ui.components.AlbumArtImage
import com.musicorbit.android.ui.components.GlassCard
import com.musicorbit.android.ui.components.OrbitBackground
import com.musicorbit.android.ui.components.SeekBarSection
import com.musicorbit.android.i18n.AppStrings
import com.musicorbit.android.i18n.LocalStrings
import com.musicorbit.android.ui.navigation.OrbitKeys
import com.musicorbit.android.ui.navigation.orbitSharedElement
import com.musicorbit.android.ui.theme.OrbitViolet
import com.musicorbit.android.ui.theme.auroraGradient
import com.musicorbit.android.ui.util.pressableSpring
import com.musicorbit.domain.model.PlayMode
import com.musicorbit.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun MainPlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToLyrics: (Long) -> Unit
) {
    val state          by viewModel.playbackState.collectAsStateWithLifecycle()
    val sleepRemaining by viewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    val strings        = LocalStrings.current

    BackHandler { onNavigateToHome() }

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        AnimatedVisibility(visible = sleepRemaining != null) {
                            val r = sleepRemaining ?: 0L
                            val mins = (r / 60_000L).toInt()
                            val secs = ((r % 60_000L) / 1_000L).toInt()
                            AssistChip(
                                onClick     = viewModel::cancelSleepTimer,
                                label       = { Text("${strings.sleepLabel}: $mins:${secs.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateToHome) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
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

                // ── Album Art (shared-element destination, glowing frame) ──────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shadow(28.dp, RoundedCornerShape(26.dp), ambientColor = OrbitViolet, spotColor = OrbitViolet, clip = false)
                        .clip(RoundedCornerShape(26.dp))
                ) {
                    AlbumArtImage(
                        uri      = state.currentSong?.albumArtUri,
                        modifier = Modifier
                            .fillMaxSize()
                            .orbitSharedElement(OrbitKeys.AlbumArt)
                    )
                }

                // ── Song info (glass) ──────────────────────────────────────────
                GlassCard(
                    modifier       = Modifier.fillMaxWidth(),
                    shape          = RoundedCornerShape(22.dp),
                    contentPadding = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text     = state.currentSong?.title ?: strings.noSong,
                            style    = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
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
                            IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit metadata", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── SeekBar ────────────────────────────────────────────────────
                SeekBarSection(
                    position = state.currentPosition,
                    duration = state.duration,
                    onSeek   = viewModel::seekTo
                )

                // ── Main playback controls ─────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    ControlIcon(Icons.Default.SkipPrevious, strings.previous, size = 32.dp) { viewModel.skipPrevious() }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(20.dp, CircleShape, ambientColor = OrbitViolet, spotColor = OrbitViolet, clip = false)
                            .clip(CircleShape)
                            .background(auroraGradient(), CircleShape)
                            .pressableSpring(pressedScale = 0.92f, onClick = viewModel::togglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = state.isPlaying,
                            transitionSpec = {
                                (scaleIn(animationSpec = tween(180)) + fadeIn()) togetherWith
                                    (scaleOut(animationSpec = tween(180)) + fadeOut())
                            },
                            label = "playpause"
                        ) { playing ->
                            Icon(
                                imageVector        = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) strings.pause else strings.play,
                                tint               = Color.White,
                                modifier           = Modifier.size(40.dp)
                            )
                        }
                    }

                    ControlIcon(Icons.Default.SkipNext, strings.next, size = 32.dp) { viewModel.skipNext() }
                }

                Spacer(Modifier.height(12.dp))

                // ── Secondary controls ─────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    ControlIcon(Icons.Default.Tune, strings.equalizer) { onNavigateToEqualizer() }

                    FilledTonalButton(
                        onClick = { state.currentSong?.id?.let { onNavigateToLyrics(it) } },
                        shape   = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Lyrics, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.lyrics)
                    }

                    // Play-mode chip: tap cycles Sequential → Shuffle → Repeat One and applies it live.
                    val (modeIcon, modeText) = when (state.playMode) {
                        PlayMode.REPEAT_ONE -> Icons.Default.RepeatOne to strings.modeRepeatOne
                        PlayMode.SHUFFLE    -> Icons.Default.Shuffle to strings.modeShuffle
                        PlayMode.SEQUENTIAL -> Icons.Default.Repeat to strings.modeSequential
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier
                            .clip(CircleShape)
                            .background(
                                if (state.playMode == PlayMode.SEQUENTIAL)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                else
                                    OrbitViolet.copy(alpha = 0.22f)
                            )
                            .pressableSpring(onClick = viewModel::cyclePlayMode)
                    ) {
                        Icon(
                            modeIcon,
                            contentDescription = "${strings.modePrefix}: $modeText",
                            tint               = if (state.playMode == PlayMode.SEQUENTIAL)
                                MaterialTheme.colorScheme.onSurface else OrbitViolet,
                            modifier           = Modifier.padding(start = 12.dp).size(22.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            modeText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 14.dp)
                        )
                    }
                }
            }
        }
    }

    // ── Edit metadata bottom sheet ──────────────────────────────────────────
    if (showEditDialog && state.currentSong != null) {
        EditSongBottomSheet(
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
private fun ControlIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    size: androidx.compose.ui.unit.Dp = 26.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .pressableSpring(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint               = MaterialTheme.colorScheme.onSurface,
            modifier           = Modifier.size(size)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSongBottomSheet(
    song: Song,
    onSave: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title      by remember { mutableStateOf(song.title) }
    var artist     by remember { mutableStateOf(song.artist) }
    var albumArt   by remember { mutableStateOf(song.albumArtUri) }
    val context    = LocalContext.current
    val strings    = LocalStrings.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { albumArt = it.toString() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
            Spacer(Modifier.height(20.dp))

            Text(
                text     = strings.editInfoTitle,
                style    = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (albumArt != null) {
                    AsyncImage(
                        model            = ImageRequest.Builder(context)
                            .data(albumArt)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album art",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier           = Modifier.size(48.dp),
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = strings.changePhoto,
                        modifier           = Modifier.size(16.dp),
                        tint               = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text(strings.songTitleLabel) },
                leadingIcon   = { Icon(Icons.Default.Audiotrack, contentDescription = null) },
                singleLine    = true,
                shape         = RoundedCornerShape(14.dp),
                modifier      = Modifier.fillMaxWidth(),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value         = artist,
                onValueChange = { artist = it },
                label         = { Text(strings.artistLabel) },
                leadingIcon   = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine    = true,
                shape         = RoundedCornerShape(14.dp),
                modifier      = Modifier.fillMaxWidth(),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.height(28.dp))

            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) { Text(strings.cancel) }

                Button(
                    onClick  = {
                        if (title.isNotBlank()) {
                            onSave(song.copy(
                                title       = title.trim(),
                                artist      = artist.trim().ifBlank { song.artist },
                                albumArtUri = albumArt
                            ))
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    enabled  = title.isNotBlank()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.save)
                }
            }
        }
    }
}
