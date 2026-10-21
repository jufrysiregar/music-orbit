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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.musicorbit.android.i18n.LocalStrings
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
    val strings      = LocalStrings.current
    val theme        by viewModel.theme.collectAsStateWithLifecycle()
    val sleepMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val language     by viewModel.language.collectAsStateWithLifecycle()

    var showThemeDialog    by remember { mutableStateOf(false) }
    var showTimerDialog    by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val themeSubtitle = when (theme) {
        "LIGHT" -> strings.themeLight
        "DARK"  -> strings.themeDark
        else    -> strings.themeSystem
    }
    val sleepSubtitle =
        if (sleepMinutes == 0) strings.sleepOff else "$sleepMinutes ${strings.minutesUnit}"
    val languageSubtitle =
        if (language == "en") strings.langEnglish else strings.langIndonesian

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title  = { Text(strings.settingsTitle) },
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsItem(Icons.Default.NightsStay, strings.sleepTimer,
                    subtitle = sleepSubtitle,
                    onClick  = { showTimerDialog = true })

                SettingsItem(Icons.Default.Palette, strings.themeTitle,
                    subtitle = themeSubtitle,
                    onClick  = { showThemeDialog = true })

                SettingsItem(Icons.Default.Language, strings.languageOption,
                    subtitle = languageSubtitle,
                    onClick  = { showLanguageDialog = true })

                SettingsItem(Icons.Default.Info, strings.aboutApp, onClick = onNavigateToAbout)
            }
        }
    }

    // ── Theme dialog ──────────────────────────────────────────────────────────
    if (showThemeDialog) {
        val options = listOf(
            "LIGHT" to strings.themeLight,
            "DARK"  to strings.themeDark,
            "SYSTEM" to strings.themeSystem
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title   = { Text(strings.pickTheme) },
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

    // ── Language dialog ─────────────────────────────────────────────────────
    if (showLanguageDialog) {
        val options = listOf(
            "id" to strings.langIndonesian,
            "en" to strings.langEnglish
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title   = { Text(strings.languageTitle) },
            text    = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            modifier          = Modifier.fillMaxWidth().clickable {
                                viewModel.saveLanguage(value)
                                showLanguageDialog = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = language == value, onClick = {
                                viewModel.saveLanguage(value); showLanguageDialog = false
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
        val options = listOf(0, 15, 30, 45, 60, 90, 120)
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title   = { Text(strings.sleepTitle) },
            text    = {
                Column {
                    Text(
                        strings.sleepDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    options.forEach { value ->
                        val label =
                            if (value == 0) strings.sleepOff else "$value ${strings.minutesUnit}"
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
