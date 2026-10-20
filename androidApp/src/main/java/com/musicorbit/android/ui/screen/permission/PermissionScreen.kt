package com.musicorbit.android.ui.screen.permission

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.musicorbit.android.R
import com.musicorbit.android.i18n.LocalStrings
import com.musicorbit.android.ui.components.GlassCard
import com.musicorbit.android.ui.components.OrbitBackground
import com.musicorbit.android.ui.theme.OrbitViolet
import com.musicorbit.domain.repository.PreferencesRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * First-run gate. Automatically requests audio access and (on Android 13+) notification access
 * as soon as it appears — there is no "grant" button. Shows a lightweight splash while the
 * system dialogs are up, and offers a language switch right on this landing screen.
 *
 * Once audio access is granted, [content] is rendered.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(content: @Composable () -> Unit) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val audioState = rememberPermissionState(permission)
    val notifState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    // Fire the audio request automatically on entry / re-entry.
    LaunchedEffect(audioState.status.isGranted) {
        if (!audioState.status.isGranted) audioState.launchPermissionRequest()
    }

    if (audioState.status.isGranted) {
        // Ask for notifications once (non-blocking), then reveal the app.
        LaunchedEffect(Unit) {
            if (notifState != null && !notifState.status.isGranted) {
                notifState.launchPermissionRequest()
            }
        }
        content()
    } else {
        PermissionSplash()
    }
}

@Composable
private fun PermissionSplash() {
    val strings = LocalStrings.current
    val prefs: PreferencesRepository = koinInject()
    val lang by prefs.getLanguage().collectAsStateWithLifecycle(initialValue = "id")
    val scope  = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitBackground()

        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier       = Modifier.padding(horizontal = 28.dp),
                shape          = RoundedCornerShape(28.dp),
                contentPadding = 28.dp,
                glow           = true
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = OrbitViolet, spotColor = OrbitViolet, clip = false)
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        Image(
                            painter            = painterResource(R.drawable.app_logo),
                            contentDescription = "Music Orbit",
                            modifier           = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text      = strings.permissionTitle,
                        style     = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text      = strings.permissionBody,
                        style     = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Language switch, right on the landing screen. Label shows the language to switch TO.
        LanguageToggle(
            currentLang = lang,
            modifier    = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) { newLang ->
            scope.launch { prefs.saveLanguage(newLang) }
        }
    }
}

@Composable
private fun LanguageToggle(
    currentLang: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    val target = if (currentLang == "en") "Bahasa Indonesia" else "English"
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .heightIn(min = 40.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { onSelect(if (currentLang == "en") "id" else "en") }) {
            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(target)
        }
    }
}
