package com.musicorbit.android.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.Color as AndroidColor
import android.net.Uri
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
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.musicorbit.android.MainActivity
import com.musicorbit.android.R
import com.musicorbit.android.service.MusicService
import com.musicorbit.domain.repository.SongRepository
import com.musicorbit.platform.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Home-screen widget matching the reference layout:
 *  - Left:        current song album art (decoded from its content URI).
 *  - Top-Left:    song title + artist.
 *  - Top-Right:   the app logo, which launches [MainActivity] when tapped.
 *  - Center-Bottom: previous / play-pause / next, wired to [AudioPlayer] via a broadcast.
 *
 * It renders purely from [NowPlayingStore], so it never touches ExoPlayer off the main thread,
 * and repaints in real time whenever the store changes (song change or play/pause).
 */
class MusicWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = runCatching { GlobalContext.get().get<NowPlayingStore>().state.value }
            .getOrDefault(NowPlayingStore.NowPlaying())

        val playIcon = if (snapshot.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        val albumArt = loadAlbumArt(context, snapshot.albumArtUri)
        val artProvider = albumArt?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.app_logo)
        // Real launcher icon for the top-right badge (matches the app's drawer icon exactly).
        val launcherProvider = loadLauncherIcon(context)?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.app_logo)

        provideContent {
            val ctx = LocalContext.current
            // Deep link that opens MainActivity straight into the Main Player screen.
            val openPlayerAction = actionStartActivity(
                Intent(ctx, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
                    .putExtra(MainActivity.EXTRA_TRACK_ID, snapshot.trackId ?: -1L)
            )
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(AndroidColor.parseColor("#12101F")))
                    .cornerRadius(20.dp)
                    .padding(12.dp)
            ) {
                // ── Header: album art | title/artist | app launcher ──────────────
                Row(
                    modifier              = GlanceModifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Album art → open the Main Player screen for the current track.
                    Box(
                        modifier         = GlanceModifier
                            .size(56.dp)
                            .cornerRadius(12.dp)
                            .clickable(openPlayerAction),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider           = artProvider,
                            contentDescription = if (snapshot.hasSong) snapshot.title else "Music Orbit"
                        )
                    }

                    Spacer(GlanceModifier.width(12.dp))

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
                            text     = if (snapshot.hasSong) snapshot.artist else "Tap play to start",
                            maxLines = 1,
                            style    = TextStyle(
                                color    = ColorProvider(AndroidColor.parseColor("#B8B5C9")),
                                fontSize = 13.sp
                            )
                        )
                    }

                    Spacer(GlanceModifier.width(6.dp))

                    // App launcher icon (circular) → opens the Main Player via deep link.
                    Box(
                        modifier = GlanceModifier
                            .size(34.dp)
                            .cornerRadius(17.dp)
                            .clickable(openPlayerAction),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider           = launcherProvider,
                            contentDescription = "Open Music Orbit"
                        )
                    }
                }

                // Push the transport controls to the bottom of the widget.
                Spacer(GlanceModifier.height(10.dp))

                // ── Transport controls, centered along the bottom ────────────────
                // Glance's Row has no horizontalArrangement; center the cluster with weighted spacers.
                Row(
                    modifier              = GlanceModifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Spacer(GlanceModifier.defaultWeight())
                    WidgetButton(ctx, R.drawable.ic_widget_prev, "Previous", "prev", 44.dp)
                    Spacer(GlanceModifier.width(10.dp))
                    WidgetButton(ctx, playIcon, "Play/Pause", "toggle", 54.dp)
                    Spacer(GlanceModifier.width(10.dp))
                    WidgetButton(ctx, R.drawable.ic_widget_next, "Next", "next", 44.dp)
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
    }

    @Composable
    private fun WidgetButton(
        context: Context,
        @androidx.annotation.DrawableRes icon: Int,
        desc: String,
        action: String,
        size: androidx.compose.ui.unit.Dp
    ) {
        Box(
            modifier = GlanceModifier
                .size(size)
                .cornerRadius(size / 2)
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(ACTION_EXTRA) ?: return
        runCatching {
            // Make sure the MediaSessionService is running so playback runs as a proper foreground
            // media service (notification + keep-alive controller) instead of a bare background player.
            context.startService(Intent(context, MusicService::class.java))
            val audioPlayer = GlobalContext.get().get<AudioPlayer>()
            val store       = GlobalContext.get().get<NowPlayingStore>()
            when (action) {
                "prev"   -> audioPlayer.skipPrevious()
                "next"   -> audioPlayer.skipNext()
                "toggle" -> if (audioPlayer.isPlayIntent()) audioPlayer.pause() else audioPlayer.resume()
            }
            store.setPlaying(audioPlayer.isPlayIntent())

            // Republish full metadata (title / artist / art) from the player so the widget also
            // updates on track change even when the UI ViewModel is not alive to observe it.
            val mediaId = audioPlayer.getCurrentMediaId()
            if (mediaId != null) {
                scope.launch {
                    val song = GlobalContext.get().get<SongRepository>().getSongById(mediaId)
                    if (song != null) {
                        store.update(
                            title       = song.title,
                            artist      = song.artist,
                            isPlaying   = audioPlayer.isPlayIntent(),
                            hasSong     = true,
                            albumArtUri = song.albumArtUri,
                            trackId     = song.id
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_EXTRA = "mo_action"
    }
}

/**
 * Decode the album-art content URI to a downscaled [Bitmap] for the widget. Returns null on any
 * failure so the caller can fall back to the app logo. Runs on the Glance render coroutine (off
 * the main thread), so bitmap decoding here is safe.
 */
private fun loadAlbumArt(context: Context, uri: String?, maxPx: Int = 256): Bitmap? {
    if (uri.isNullOrBlank()) return null
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(Uri.parse(uri))?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > maxPx) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(Uri.parse(uri))?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }.getOrNull()
}

/**
 * Render the app's actual launcher icon (as shown in the drawer) to a [Bitmap] for the widget
 * badge. Falls back to null on any failure so the caller can use the bundled app_logo.
 */
private fun loadLauncherIcon(context: Context, sizePx: Int = 128): Bitmap? = runCatching {
    val drawable = context.packageManager.getApplicationIcon(context.packageName)
    drawableToBitmap(drawable, sizePx)
}.getOrNull()

private fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
