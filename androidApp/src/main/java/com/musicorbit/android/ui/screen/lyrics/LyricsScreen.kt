package com.musicorbit.android.ui.screen.lyrics

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicorbit.android.i18n.LocalStrings
import com.musicorbit.android.ui.components.GlassCard
import com.musicorbit.android.ui.components.OrbitBackground
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    songId: Long,
    onBack: () -> Unit,
    viewModel: LyricsViewModel = koinViewModel()
) {
    val context   = LocalContext.current
    val strings   = LocalStrings.current
    val lyrics    by viewModel.lyrics.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val song      by viewModel.song.collectAsStateWithLifecycle()

    // Purely a UI concern: view mode vs inline edit mode inside the SAME container.
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(songId) { viewModel.loadLyrics(songId) }

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title  = { Text(song?.title ?: strings.lyrics) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.buildSearchUrl())))
                        }) {
                            Icon(Icons.Default.Search, contentDescription = strings.searchLyricsGoogle)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Section header + inline-edit trigger (pencil) ────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = strings.lyrics,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isEditing) {
                        IconButton(onClick = {
                            // Seed the editor with the current lyrics before switching to edit mode.
                            viewModel.onInputTextChange(lyrics?.lyricsText ?: "")
                            isEditing = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = strings.editLyrics)
                        }
                    }
                }

                // ── Single consolidated lyrics container ─────────────────────────────
                GlassCard(shape = RoundedCornerShape(22.dp), contentPadding = 18.dp, glow = true) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (isEditing) {
                            OutlinedTextField(
                                value         = inputText,
                                onValueChange = viewModel::onInputTextChange,
                                label         = { Text(strings.pasteHere) },
                                placeholder   = { Text(strings.pastePlaceholder) },
                                shape         = RoundedCornerShape(16.dp),
                                modifier      = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 260.dp),
                                minLines      = 10
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        // Discard unsaved changes and revert to the stored lyrics.
                                        viewModel.onInputTextChange(lyrics?.lyricsText ?: "")
                                        isEditing = false
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ) { Text(strings.cancel) }

                                Button(
                                    onClick = {
                                        viewModel.saveLyrics(songId)
                                        isEditing = false
                                    },
                                    enabled = inputText.isNotBlank(),
                                    shape   = RoundedCornerShape(14.dp)
                                ) { Text(strings.saveLyrics) }
                            }
                        } else if (lyrics != null) {
                            Text(
                                text  = lyrics!!.lyricsText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text  = strings.noLyricsHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = {
                                viewModel.onInputTextChange("")
                                isEditing = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.editOrPaste)
                            }
                        }
                    }
                }
            }
        }
    }
}
