package com.musicorbit.android.ui.screen.lyrics

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val lyrics    by viewModel.lyrics.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val song      by viewModel.song.collectAsStateWithLifecycle()

    LaunchedEffect(songId) { viewModel.loadLyrics(songId) }

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title  = { Text(song?.title ?: "Lirik") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.buildSearchUrl())))
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Cari Lirik di Google")
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (lyrics != null) {
                    GlassCard(shape = RoundedCornerShape(22.dp), contentPadding = 18.dp, glow = true) {
                        Text(
                            text  = lyrics!!.lyricsText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text("Edit atau tempel lirik baru:", style = MaterialTheme.typography.labelMedium)
                } else {
                    Text(
                        text  = "Belum ada lirik. Cari di Google lalu tempel di bawah.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value        = inputText,
                    onValueChange = viewModel::onInputTextChange,
                    label        = { Text("Tempel lirik di sini...") },
                    placeholder  = { Text("Salin lirik dari browser, lalu tempel di sini") },
                    shape        = RoundedCornerShape(18.dp),
                    modifier     = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    minLines     = 8
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.buildSearchUrl())))
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Cari Lirik")
                    }

                    Button(
                        onClick  = { viewModel.saveLyrics(songId) },
                        enabled  = inputText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(14.dp)
                    ) {
                        Text("Simpan Lirik")
                    }
                }
            }
        }
    }
}
