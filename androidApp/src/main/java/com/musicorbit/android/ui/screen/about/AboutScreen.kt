package com.musicorbit.android.ui.screen.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tentang Aplikasi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier              = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.Center
        ) {
            // App name
            Text(
                text      = "Music Orbit",
                style     = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Versi 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            // Developer info (exact text as specified in Requirement 8)
            Text(
                text      = "MJS - Universitas Potensi Utama '19 - Informatika",
                style     = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(32.dp))

            // 3 clickable links in a Row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // LinkedIn
                OutlinedButton(
                    onClick = { openUrl("https://www.linkedin.com/in/mjuffrisiregar/") }
                ) {
                    Text("LinkedIn")
                }

                // Email
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("mailto:mjuffrisiregar@gmail.com"))
                        )
                    }
                ) {
                    Text("Email")
                }

                // Portfolio
                OutlinedButton(
                    onClick = { openUrl("https://mjs-portofolio.vercel.app/") }
                ) {
                    Text("porto.")
                }
            }
        }
    }
}
