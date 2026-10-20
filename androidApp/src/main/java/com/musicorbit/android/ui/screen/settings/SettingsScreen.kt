package com.musicorbit.android.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import java.util.Locale
import com.musicorbit.android.ui.components.GlassCard
import com.musicorbit.android.ui.components.OrbitBackground
import com.musicorbit.android.ui.util.pressableSpring

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val theme        by viewModel.theme.collectAsStateWithLifecycle()
    val sleepMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val language     by viewModel.language.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showLangDialog  by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title  = { Text("Pengaturan") },
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsItem(Icons.Default.Language, "Bahasa / Language",
                    subtitle = when (language) { "en" -> "English"; else -> "Indonesia" },
                    onClick  = { showLangDialog = true })

                SettingsItem(Icons.Default.NightsStay, "Pengaturan Waktu Tidur",
                    subtitle = when (sleepMinutes) {
                        0   -> "Tidak aktif"
                        15  -> "15 menit"
                        30  -> "30 menit"
                        45  -> "45 menit"
                        60  -> "1 jam"
                        90  -> "1,5 jam"
                        120 -> "2 jam"
                        else -> "$sleepMinutes menit"
                    },
                    onClick = { showTimerDialog = true })

                SettingsItem(Icons.Default.Palette, "Tema",
                    subtitle = when (theme) { "LIGHT" -> "Terang"; "DARK" -> "Gelap"; else -> "Ikuti Sistem" },
                    onClick  = { showThemeDialog = true })

                SettingsItem(Icons.Default.Info, "Tentang Aplikasi", onClick = onNavigateToAbout)

                // ── All-files access (Android 11+ only, for .lrc / .txt lyrics) ──
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    var hasAllFilesAccess by remember { mutableStateOf(Environment.isExternalStorageManager()) }
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                        hasAllFilesAccess = Environment.isExternalStorageManager()
                    }
                    SettingsItem(
                        icon     = Icons.Default.FolderOpen,
                        title    = "Izin Akses File Lirik",
                        subtitle = if (hasAllFilesAccess)
                            "Diberikan — file .lrc/.txt akan otomatis terdeteksi"
                        else
                            "Belum diberikan — diperlukan untuk baca file lirik di penyimpanan",
                        onClick  = {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.parse("package:${context.packageName}")
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    )
                }
            }
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
                                viewModel.saveTheme(value); showThemeDialog = false
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
        val options = listOf(
            0 to "Tidak aktif", 15 to "15 menit", 30 to "30 menit", 45 to "45 menit",
            60 to "1 jam", 90 to "1,5 jam", 120 to "2 jam"
        )
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title   = { Text("Waktu Tidur") },
            text    = {
                Column {
                    Text(
                        "Musik akan berhenti otomatis setelah waktu yang dipilih.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    options.forEach { (value, label) ->
                        Row(
                            modifier          = Modifier.fillMaxWidth().clickable {
                                viewModel.saveSleepTimer(value); showTimerDialog = false
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = sleepMinutes == value, onClick = {
                                viewModel.saveSleepTimer(value); showTimerDialog = false
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // ── Language dialog ──────────────────────────────────────────────────────
    if (showLangDialog) {
        val options = listOf("id" to "Bahasa Indonesia", "en" to "English")
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title   = { Text("Pilih Bahasa") },
            text    = {
                Column {
                    options.forEach { (value, label) ->
                        val applyLang = {
                            viewModel.saveLanguage(value)
                            val locale = Locale(value)
                            Locale.setDefault(locale)
                            val config = android.content.res.Configuration(context.resources.configuration)
                            config.setLocale(locale)
                            context.createConfigurationContext(config)
                            (context as? Activity)?.recreate()
                            showLangDialog = false
                        }
                        Row(
                            modifier          = Modifier.fillMaxWidth().clickable(onClick = applyLang).padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = language == value, onClick = applyLang)
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
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    GlassCard(
        modifier       = Modifier
            .fillMaxWidth()
            .pressableSpring(onClick = onClick),
        shape          = RoundedCornerShape(18.dp),
        contentPadding = 6.dp
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier       = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier           = Modifier.size(16.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
