package com.musicorbit.android.ui.screen.equalizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
            // Enable / Disable toggle
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Aktifkan Equalizer", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked         = state.isEnabled,
                    onCheckedChange = viewModel::onEnabledChanged
                )
            }

            HorizontalDivider()

            // Preset chips
            Text("Preset", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.presets.forEach { preset ->
                    FilterChip(
                        selected = preset == state.selectedPreset,
                        onClick  = { viewModel.onPresetSelected(preset) },
                        label    = { Text(preset) },
                        enabled  = state.isEnabled
                    )
                }
            }

            HorizontalDivider()

            // Band sliders
            if (state.bandLevels.isEmpty()) {
                Text(
                    text  = "Equalizer tidak tersedia untuk sesi audio ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text("Band Frekuensi", style = MaterialTheme.typography.labelLarge)
                state.bandLevels.forEachIndexed { index, level ->
                    val freqLabel = if (index < state.bandFrequencies.size)
                        "${state.bandFrequencies[index]} Hz" else "Band ${index + 1}"

                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text     = freqLabel,
                            style    = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(64.dp)
                        )
                        Slider(
                            value             = level.toFloat(),
                            onValueChange     = { viewModel.onBandLevelChange(index, it.toInt()) },
                            valueRange        = -1500f..1500f,
                            steps             = 0,
                            enabled           = state.isEnabled,
                            modifier          = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
