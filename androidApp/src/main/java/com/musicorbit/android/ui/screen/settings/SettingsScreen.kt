package com.musicorbit.android.ui.screen.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    var showCustomDialog   by remember { mutableStateOf(false) }
    var customInput        by remember { mutableStateOf("") }

    // Battery-optimization exemption: read live status and refresh whenever the screen resumes
    // (the user may toggle it in the system dialog and come straight back here).
    val context = LocalContext.current
    var isIgnoringBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBattery = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

                SettingsItem(Icons.Default.BatterySaver, strings.batteryOption,
                    subtitle = if (isIgnoringBattery) strings.batteryStatusOn else strings.batteryStatusOff,
                    onClick  = { showBatteryDialog = true })

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
        val options = listOf(0, 5, 15, 30, 60)
        val isCustomActive = sleepMinutes != 0 && sleepMinutes !in options
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
                    // Custom option -> opens a numeric input dialog.
                    Row(
                        modifier          = Modifier.fillMaxWidth().clickable {
                            customInput = if (isCustomActive) sleepMinutes.toString() else ""
                            showTimerDialog = false
                            showCustomDialog = true
                        }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isCustomActive, onClick = {
                            customInput = if (isCustomActive) sleepMinutes.toString() else ""
                            showTimerDialog = false
                            showCustomDialog = true
                        })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isCustomActive) "$sleepMinutes ${strings.minutesUnit}" else strings.sleepCustom,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    // ── Custom sleep duration dialog (numeric only) ───────────────────────────
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title   = { Text(strings.sleepCustom) },
            text    = {
                OutlinedTextField(
                    value           = customInput,
                    onValueChange   = { customInput = it.filter { c -> c.isDigit() }.take(3) },
                    label           = { Text(strings.minutesUnit) },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape           = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                val minutes = customInput.toIntOrNull() ?: 0
                TextButton(
                    enabled = minutes > 0,
                    onClick = {
                        viewModel.saveSleepTimer(minutes)
                        showCustomDialog = false
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // ── Battery optimization exemption dialog ────────────────────────────────
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title   = { Text(strings.batteryOption) },
            text    = { Text(strings.batteryDesc) },
            confirmButton = {
                TextButton(onClick = {
                    requestIgnoreBatteryOptimizations(context)
                    showBatteryDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

/** True when the app is already exempt from battery optimization (or on pre-Marshmallow). */
private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
}

/**
 * Ask the system to exempt this app from battery optimization. Prefers the direct
 * ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS dialog; falls back to the global battery-optimization
 * settings list if the device has no such dialog (some OEM ROMs) or it cannot be launched.
 */
private fun requestIgnoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val pkg = context.packageName
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        .setData(Uri.parse("package:$pkg"))
    if (runCatching { context.startActivity(direct) }.isSuccess) return
    runCatching {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
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
