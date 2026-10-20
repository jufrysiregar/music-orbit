package com.musicorbit.android.ui.screen.equalizer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicorbit.android.i18n.LocalStrings
import com.musicorbit.android.ui.components.GlassCard
import com.musicorbit.android.ui.components.OrbitBackground
import com.musicorbit.android.ui.screen.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        viewModel.initialize(playerViewModel.audioSessionId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title  = { Text(strings.equalizer) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
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
                // Enable toggle
                GlassCard(shape = RoundedCornerShape(20.dp), contentPadding = 14.dp) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(strings.enableEqualizer, style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked         = state.isEnabled,
                            onCheckedChange = viewModel::onEnabledChanged
                        )
                    }
                }

                // Preset chips
                GlassCard(shape = RoundedCornerShape(20.dp), contentPadding = 14.dp) {
                    Column {
                        Text(strings.preset, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.presets.forEach { preset ->
                                FilterChip(
                                    selected  = preset == state.selectedPreset,
                                    onClick   = { viewModel.onPresetSelected(preset) },
                                    label     = { Text(preset) },
                                    enabled   = state.isEnabled,
                                    shape     = RoundedCornerShape(14.dp)
                                )
                            }
                        }
                    }
                }

                // Band sliders
                if (state.bandLevels.isEmpty()) {
                    GlassCard(shape = RoundedCornerShape(20.dp), contentPadding = 16.dp) {
                        Text(
                            text  = strings.eqUnavailable,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    GlassCard(shape = RoundedCornerShape(20.dp), contentPadding = 16.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(strings.bandFrequencies, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            state.bandLevels.forEachIndexed { index, level ->
                                val freqLabel = if (index < state.bandFrequencies.size) {
                                    val freq = state.bandFrequencies[index]
                                    if (freq >= 1000) "${freq / 1000} kHz" else "$freq Hz"
                                } else "${strings.bandWord} ${index + 1}"

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Text(text = freqLabel, style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            text  = "${level / 100} dB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value         = level.toFloat(),
                                        onValueChange = { viewModel.onBandLevelChange(index, it.toInt()) },
                                        valueRange    = state.minLevel.toFloat()..state.maxLevel.toFloat(),
                                        enabled       = state.isEnabled,
                                        modifier      = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
