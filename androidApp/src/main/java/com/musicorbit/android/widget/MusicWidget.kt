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
import androidx.glance.layout.ContentScale
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
import com.musicorbit.android.MainActivity
import com.musicorbit.android.R
import com.musicorbit.android.service.MusicService
import com.musicorbit.android.di.androidModule
import com.musicorbit.di.sharedModule
import com.musicorbit.domain.repository.PreferencesRepository
import com.musicorbit.domain.repository.SongRepository
import com.musicorbit.platform.AudioPlayer
import com.musicorbit.platform.toQueueItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Home-screen widget matching the reference layout:
 *  - Left:        current song album art (decoded from its content URI).
 *  - Middle:      song title + artist.
 *  - Right:       previous / play-pause / next transport controls.
 *
 * Renders purely from [NowPlayingStore]; never touches ExoPlayer off the main thread.
 * Repaints whenever the store changes (song change or play/pause).
 *
 * Fix notes:
 *  - SizeMode.Responsive instead of SizeMode.Exact to avoid crash on first placement.
 *  - ensureKoinStarted() guard so widget works even if the app process was never opened.
 *  - actionSendBroadcast uses ComponentName string (not class ref) safe for all API levels.
 */
class MusicWidget : GlanceAppWidget() {

    // SizeMode.Responsive: define supported sizes so Glance picks the best fit.
    // This avoids the "cannot load widget" crash that SizeMode.Exact causes on first placement
    // when the system hasn't yet delivered a valid size to the widget host.
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // ─── DIAGNOSTIC: absolute minimum content to test whether Glance can render AT ALL ───
        // No Koin, no bitmap, no store, no complex layout. Just a static Text.
        // If this still shows "cannot load", the issue is environmental (Glance setup / OEM).
        // If it loads, we add features back one at a time to find the crash point.
        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(AndroidColor.parseColor("#12101F")))
                    .cornerRadius(20.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "Music Orbit",
                    style = TextStyle(
                        color      = ColorProvider(AndroidColor.WHITE),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    /* ─── FULL VERSION (restore after diagnostic passes) ───
    override suspend fun provideGlanceDiag(context: Context, id: GlanceId) {
        ensureKoinStarted(context)

        val snapshot = runCatching {
            GlobalContext.get().get<NowPlayingStore>().state.value
        }.getOrDefault(NowPlayingStore.NowPlaying())

        val playIcon    = if (snapshot.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        val albumArt    = loadAlbumArt(context, snapshot.albumArtUri)
        val artProvider = albumArt?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.app_logo)

        provideContent {
            val ctx = LocalContext.current
            WidgetContent(ctx, snapshot, artProvider, playIcon)
        }
    }
    */

    @Composable
    private fun WidgetContent(
        ctx: Context,
        snapshot: NowPlayingStore.NowPlaying,
        artProvider: ImageProvider,
        playIcon: Int
    ) {
        val openPlayerAction = actionStartActivity(
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
                .putExtra(MainActivity.EXTRA_TRACK_ID, snapshot.trackId ?: -1L)
        )

        Row(
            modifier          = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(AndroidColor.parseColor("#12101F")))
                .cornerRadius(20.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art — tap opens the player screen
            Box(
                modifier         = GlanceModifier
                    .size(52.dp)
                    .cornerRadius(12.dp)
                    .clickable(openPlayerAction),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider           = artProvider,
                    contentDescription = if (snapshot.hasSong) snapshot.title else "Music Orbit",
                    modifier           = GlanceModifier.size(52.dp),
                    contentScale       = ContentScale.Crop
                )
            }

            Spacer(GlanceModifier.width(12.dp))

            // Song title + artist
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text     = if (snapshot.hasSong) snapshot.title else "Music Orbit",
                    maxLines = 1,
                    style    = TextStyle(
                        color      = ColorProvider(AndroidColor.WHITE),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                )
                Text(
                    text     = if (snapshot.hasSong) snapshot.artist else "Tap play to start",
                    maxLines = 1,
                    style    = TextStyle(
                        color    = ColorProvider(AndroidColor.parseColor("#B8B5C9")),
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(GlanceModifier.width(6.dp))

            // Transport controls
            WidgetButton(ctx, R.drawable.ic_widget_prev,  "Previous",   ACTION_PREV,   40.dp)
            Spacer(GlanceModifier.width(6.dp))
            WidgetButton(ctx, playIcon,                   "Play/Pause",  ACTION_TOGGLE, 46.dp)
            Spacer(GlanceModifier.width(6.dp))
            WidgetButton(ctx, R.drawable.ic_widget_next,  "Next",        ACTION_NEXT,   40.dp)
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
                        Intent(WidgetActionReceiver.ACTION_WIDGET_CONTROL)
                            .setPackage(context.packageName)
                            .putExtra(WidgetActionReceiver.EXTRA_ACTION, action)
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

    companion object {
        const val ACTION_PREV   = "prev"
        const val ACTION_TOGGLE = "toggle"
        const val ACTION_NEXT   = "next"
    }
}

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget()
}

/**
 * Receives transport taps from the widget and forwards them to [AudioPlayer].
 *
 * Uses an implicit broadcast action ([ACTION_WIDGET_CONTROL]) + package filter instead of
 * an explicit class-based Intent, which is safer across API levels and avoids the
 * PendingIntent mutability restrictions on Android 12+.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WIDGET_CONTROL) return
        val action = intent.getStringExtra(EXTRA_ACTION) ?: return

        ensureKoinStarted(context)

        runCatching {
            // Ensure the foreground media service is running.
            context.startService(Intent(context, MusicService::class.java))

            val audioPlayer = GlobalContext.get().get<AudioPlayer>()
            val store       = GlobalContext.get().get<NowPlayingStore>()

            // Cold start: player queue is empty — rebuild from last-played track.
            if (!audioPlayer.hasMediaLoaded()) {
                scope.launch { restoreQueueAndPlay(audioPlayer, store) }
                return
            }

            when (action) {
                MusicWidget.ACTION_PREV   -> audioPlayer.skipPrevious()
                MusicWidget.ACTION_NEXT   -> audioPlayer.skipNext()
                MusicWidget.ACTION_TOGGLE -> if (audioPlayer.isPlayIntent()) audioPlayer.pause()
                                             else audioPlayer.resume()
            }
            store.setPlaying(audioPlayer.isPlayIntent())

            // Republish full metadata so the widget title/art also updates on track change.
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

    private suspend fun restoreQueueAndPlay(audioPlayer: AudioPlayer, store: NowPlayingStore) {
        val repo  = GlobalContext.get().get<SongRepository>()
        val prefs = GlobalContext.get().get<PreferencesRepository>()
        val all   = repo.getAllSongs().first()
        if (all.isEmpty()) return
        val lastId = prefs.getLastPlayedSongId()
        val index  = all.indexOfFirst { it.id == lastId }.coerceAtLeast(0)
        audioPlayer.prepareQueue(all.map { it.toQueueItem() }, index, autoplay = true)
        val song = all[index]
        store.update(
            title       = song.title,
            artist      = song.artist,
            isPlaying   = true,
            hasSong     = true,
            albumArtUri = song.albumArtUri,
            trackId     = song.id
        )
    }

    companion object {
        const val ACTION_WIDGET_CONTROL = "com.musicorbit.android.WIDGET_CONTROL"
        const val EXTRA_ACTION          = "mo_action"
    }
}

// ─── Koin guard ──────────────────────────────────────────────────────────────

/**
 * Ensures Koin is initialized before any widget code accesses [GlobalContext].
 *
 * When Android creates a fresh process just to service a widget update (e.g. after reboot
 * or process-death), [MusicOrbitApp.onCreate] may not have run yet. Calling
 * [GlobalContext.get] in that state throws an [IllegalStateException] which Glance
 * catches and converts into the "cannot load widget" error message.
 *
 * This function is idempotent — it is safe to call multiple times.
 */
internal fun ensureKoinStarted(context: Context) {
    if (GlobalContext.getOrNull() != null) return   // already running — fast path
    runCatching {
        startKoin {
            androidContext(context.applicationContext)
            modules(sharedModule, androidModule)
        }
    }
    // If startKoin throws because another thread already started it, that is fine.
}

// ─── Bitmap helpers ──────────────────────────────────────────────────────────

/**
 * Decode the album-art content URI to a downscaled [Bitmap] for the widget.
 * Returns null on any failure so the caller can fall back to the app logo.
 * Runs on the Glance render coroutine (off the main thread).
 */
private fun loadAlbumArt(context: Context, uri: String?, maxPx: Int = 256): Bitmap? {
    if (uri.isNullOrBlank()) return null
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(Uri.parse(uri))?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > maxPx) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(Uri.parse(uri))?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }.getOrNull()
}

private fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
