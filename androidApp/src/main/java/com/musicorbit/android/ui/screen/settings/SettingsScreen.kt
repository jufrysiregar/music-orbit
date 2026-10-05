package com.musicorbit.android.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val theme        by viewModel.theme.collectAsStateWithLifecycle()
    val sleepMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            SettingsItem(
                title    = "Bahasa",
                subtitle = "Indonesia",
                onClick  = { /* Language switch — future implementation */ }
            )
            HorizontalDivider()
            SettingsItem(
                title    = "Pengaturan Waktu Tidur",
                subtitle = if (sleepMinutes == 0) "Tidak aktif" else "$sleepMinutes menit",
                onClick  = { showTimerDialog = true }
            )
            HorizontalDivider()
            SettingsItem(
                title    = "Tema",
                subtitle = when (theme) { "LIGHT" -> "Terang"; "DARK" -> "Gelap"; else -> "Ikuti Sistem" },
                onClick  = { showThemeDialog = true }
            )
            HorizontalDivider()
            SettingsItem(
                title   = "Tentang Aplikasi",
                onClick = onNavigateToAbout
            )
        }
    }

    // ── Theme dialog ──────────────────────────────────────────────────────────
    if (showThemeDialog) {
        val options = listOf("LIGHT" to "Terang", "DARK" to "Gelap", "SYSTEM" to "Ikuti Sistem")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title   = { Text("Pilih Tema") },
            text    = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            modifier          = Modifier.fillMaxWidth().clickable {
                                viewModel.saveTheme(value)
                                showThemeDialog = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = theme == value, onClick = {
                                viewModel.saveTheme(value)
                                showThemeDialog = false
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // ── Sleep timer dialog ────────────────────────────────────────────────────
    if (showTimerDialog) {
        val options = listOf(0 to "Tidak aktif", 15 to "15 menit", 30 to "30 menit", 45 to "45 menit", 60 to "60 menit")
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title   = { Text("Waktu Tidur") },
            text    = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            modifier          = Modifier.fillMaxWidth().clickable {
                                viewModel.saveSleepTimer(value)
                                showTimerDialog = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = sleepMinutes == value, onClick = {
                                viewModel.saveSleepTimer(value)
                                showTimerDialog = false
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun SettingsItem(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
            modifier = Modifier.size(16.dp))
    }
}
