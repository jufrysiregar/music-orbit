package com.musicorbit.android.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.musicorbit.android.R
import com.musicorbit.platform.AudioPlayer
import org.koin.core.context.GlobalContext

/**
 * Rectangular home-screen widget: app artwork on the left, the current track title/artist,
 * and previous / play-pause / next transport buttons. Renders purely from [NowPlayingStore]
 * so it never touches ExoPlayer off the main thread. Transport taps are delivered to
 * [WidgetActionReceiver] via an explicit broadcast (runs on the main thread).
 */
class MusicWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = runCatching { GlobalContext.get().get<NowPlayingStore>().state.value }
            .getOrDefault(NowPlayingStore.NowPlaying())

        val playIcon = if (snapshot.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play

        provideContent {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(AndroidColor.parseColor("#12101F")))
                    .cornerRadius(20.dp)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork ("photo")
                Box(
                    modifier         = GlanceModifier.size(56.dp).cornerRadius(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider           = ImageProvider(R.drawable.app_logo),
                        contentDescription = "Music Orbit"
                    )
                }

                Spacer(GlanceModifier.width(12.dp))

                // Title / artist
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text     = if (snapshot.hasSong) snapshot.title else "Music Orbit",
                        maxLines = 1,
                        style    = TextStyle(
                            color      = ColorProvider(AndroidColor.WHITE),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp
                        )
                    )
                    Text(
                        text     = if (snapshot.hasSong) snapshot.artist else "",
                        maxLines = 1,
                        style    = TextStyle(
                            color    = ColorProvider(AndroidColor.parseColor("#B8B5C9")),
                            fontSize = 13.sp
                        )
                    )
                }

                Spacer(GlanceModifier.width(4.dp))

                val ctx = LocalContext.current
                // Transport buttons
                WidgetButton(ctx, R.drawable.ic_widget_prev, "Previous", "prev")
                WidgetButton(ctx, playIcon,                  "Play/Pause", "toggle")
                WidgetButton(ctx, R.drawable.ic_widget_next, "Next", "next")
            }
        }
    }

    @Composable
    private fun WidgetButton(
        context: Context,
        @androidx.annotation.DrawableRes icon: Int,
        desc: String,
        action: String
    ) {
        Box(
            modifier = GlanceModifier
                .size(42.dp)
                .cornerRadius(21.dp)
                .background(ColorProvider(AndroidColor.parseColor("#2A2440")))
                .clickable(
                    actionSendBroadcast(
                        Intent(context, WidgetActionReceiver::class.java)
                            .putExtra(WidgetActionReceiver.ACTION_EXTRA, action)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider           = ImageProvider(icon),
                contentDescription = desc
            )
        }
    }
}

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget()
}

/**
 * Receives transport taps from the widget and forwards them to the shared [AudioPlayer]
 * singleton, then refreshes [NowPlayingStore] (which repaints every widget instance).
 * `onReceive` runs on the main thread, so calling the ExoPlayer-backed player is safe.
 */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(ACTION_EXTRA) ?: return
        runCatching {
            val audioPlayer = GlobalContext.get().get<AudioPlayer>()
            val store       = GlobalContext.get().get<NowPlayingStore>()
            when (action) {
                "prev"   -> audioPlayer.skipPrevious()
                "next"   -> audioPlayer.skipNext()
                "toggle" -> if (audioPlayer.isPlaying()) audioPlayer.pause() else audioPlayer.resume()
            }
            store.setPlaying(audioPlayer.isPlaying())
        }
    }

    companion object {
        const val ACTION_EXTRA = "mo_action"
    }
}
