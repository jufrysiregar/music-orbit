package com.musicorbit.android.ui.screen.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.musicorbit.android.R
import com.musicorbit.android.ui.components.GlassCard
import com.musicorbit.android.ui.components.OrbitBackground
import com.musicorbit.android.ui.theme.OrbitCyan
import com.musicorbit.android.ui.theme.OrbitMagenta
import com.musicorbit.android.ui.theme.OrbitViolet
import com.musicorbit.android.ui.theme.headlineFontFamily
import com.musicorbit.android.ui.util.pressableSpring

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title  = { Text("Tentang Aplikasi") },
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
                    .padding(24.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.Center
            ) {
                GlassCard(
                    modifier       = Modifier.fillMaxWidth(),
                    shape          = RoundedCornerShape(28.dp),
                    contentPadding = 28.dp,
                    glow           = true
                ) {
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.app_logo),
                            contentDescription = "Music Orbit",
                            modifier = Modifier
                                .size(92.dp)
                                .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = OrbitViolet, spotColor = OrbitViolet, clip = false)
                                .clip(RoundedCornerShape(22.dp))
                        )
                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = "Music Orbit",
                            style = TextStyle(
                                brush = Brush.horizontalGradient(listOf(OrbitViolet, OrbitCyan, OrbitMagenta)),
                                fontFamily = headlineFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text  = "Versi 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(28.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(20.dp))

                        Text(
                            text      = "MJS - Universitas Potensi Utama '19 - Informatika",
                            style     = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    GlassLinkButton("LinkedIn", Modifier.weight(1f)) {
                        openUrl("https://www.linkedin.com/in/mjuffrisiregar/")
                    }
                    GlassLinkButton("Email", Modifier.weight(1f)) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("mailto:mjuffrisiregar@gmail.com"))
                        )
                    }
                    GlassLinkButton("porto.", Modifier.weight(1f)) {
                        openUrl("https://mjs-portofolio.vercel.app/")
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassLinkButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(
        modifier       = modifier
            .height(52.dp)
            .pressableSpring(onClick = onClick),
        shape          = RoundedCornerShape(16.dp),
        contentPadding = 0.dp
    ) {
        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
