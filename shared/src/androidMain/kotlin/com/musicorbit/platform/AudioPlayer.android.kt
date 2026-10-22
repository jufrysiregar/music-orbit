package com.musicorbit.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.musicorbit.domain.model.PlayMode

/** Action string for the notification's Stop ("X") custom command (see buildCommandLayout). */
private const val NOTIFICATION_STOP_ACTION = "com.musicorbit.platform.action.STOP"

actual class AudioPlayer(private val context: Context) {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        // USAGE_MEDIA + CONTENT_TYPE_MUSIC with handleAudioFocus=true gives proper audio focus
        // (ducks/pauses for calls) and correct routing on the system media session.
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        // CPU wake lock held by the player itself during playback -> survives screen-off / Doze.
        setWakeMode(C.WAKE_MODE_LOCAL)
        setHandleAudioBecomingNoisy(true)
    }

    private val mediaSession: MediaSession = run {
        // Handle the notification's Stop ("X") action. Per Media3, the ongoing media notification
        // can only be dismissed once the playlist is cleared, so stopping means clearing it (which
        // also removes the card and lets the service be reclaimed). X is only shown while paused.
        val stopCallback = object : MediaSession.Callback {
            @OptIn(UnstableApi::class)
            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == NOTIFICATION_STOP_ACTION) {
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    playbackListener?.invoke(false)
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        val builder = MediaSession.Builder(context, exoPlayer)
            .setId("MusicOrbitSession")
            .setCallback(stopCallback)
        // The notification's content intent (tap anywhere on the media card) opens the running app
        // straight into the player. We resolve the launcher intent by package so the shared module
        // doesn't need to reference MainActivity.
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                putExtra("EXTRA_OPEN_PLAYER", true)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        if (launchIntent != null) {
            val pi = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setSessionActivity(pi)
        }
        val session = builder.build()
        // Initially not playing -> include the Stop ("X") button. Kept in sync on every
        // play/pause transition in the Player.Listener below.
        session.setCustomLayout(buildCommandLayout(includeStop = !exoPlayer.playWhenReady))
        session
    }

    /**
     * Custom notification layout: the usual previous / play-pause / next transport buttons, plus a
     * Stop ("X") action ONLY while [includeStop] is true (i.e. when paused). Media3 renders these
     * CommandButtons directly in the notification, and the play/pause button auto-toggles its glyph
     * because it is bound to [Player.COMMAND_PLAY_PAUSE].
     */
    @OptIn(UnstableApi::class)
    private fun buildCommandLayout(includeStop: Boolean): List<CommandButton> {
        val transport = listOf(
            CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build(),
            CommandButton.Builder(CommandButton.ICON_PLAY)
                .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                .build(),
            CommandButton.Builder(CommandButton.ICON_NEXT)
                .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .build()
        )
        if (!includeStop) return transport
        val stop = CommandButton.Builder(CommandButton.ICON_STOP)
            .setDisplayName("Stop")
            .setSessionCommand(SessionCommand(NOTIFICATION_STOP_ACTION, Bundle.EMPTY))
            .build()
        return transport + stop
    }

    // PARTIAL_WAKE_LOCK keeps the CPU awake while audio is playing, so playback doesn't stall
    // once the screen turns off / the device enters Doze. Reference-counted off for simple hold/release.
    private val wakeLock: PowerManager.WakeLock by lazy {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicOrbit:PlaybackWakeLock")
            .apply { setReferenceCounted(false) }
    }

    private var playbackListener: ((Boolean) -> Unit)? = null
    private var trackListener: ((Int) -> Unit)? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Defensive re-acquire: covers the very first play() where playWhenReady was already
                // true before this callback fires. Never release here — isPlaying flips false during
                // track transitions / buffering, and releasing then would let the CPU suspend mid-stream.
                if (isPlaying && exoPlayer.playWhenReady && !wakeLock.isHeld) {
                    wakeLock.acquire(10L * 60L * 60L * 1000L)
                }
                // Report the user's play intent (playWhenReady) rather than the raw isPlaying flag:
                // this keeps the play/pause button from flickering to the "paused" look during brief
                // buffering.
                playbackListener?.invoke(exoPlayer.playWhenReady)
            }

            // playWhenReady flips instantly on user play/pause even while the player is still
            // preparing, so the UI updates with zero delay and never shows a stale style.
            @OptIn(UnstableApi::class)
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Key the wake lock off play INTENT, not transient isPlaying: keep the CPU awake for
                // the whole time the user wants audio (survives screen-off + track gaps), and release
                // only on a real pause/stop. 10h safety timeout so a leaked lock never pins the CPU.
                if (playWhenReady) {
                    if (!wakeLock.isHeld) wakeLock.acquire(10L * 60L * 60L * 1000L)
                } else if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                // While playing hide the Stop ("X") button; while paused show it so the user can
                // dismiss the notification.
                mediaSession.setCustomLayout(buildCommandLayout(includeStop = !playWhenReady))
                playbackListener?.invoke(playWhenReady)
            }

            // Fires on Next/Prev, auto-advance, and remote control/widget actions, so the UI can
            // reflect the newly playing track in real time (not only via polling).
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                trackListener?.invoke(exoPlayer.currentMediaItemIndex)
            }
        })
    }

    /**
     * Exposes the singleton [MediaSession] so [com.musicorbit.android.service.MusicService]
     * can publish it via MediaSessionService.onGetSession(). This is what enables the system
     * media notification and background playback. There is exactly ONE session for the whole app.
     */
    fun getMediaSession(): MediaSession = mediaSession

    /** Exposed so EqualizerEngine can bind to the correct audio session. */
    val audioSessionId: Int get() = exoPlayer.audioSessionId

    /**
     * Build a MediaItem that also carries [MediaMetadata] (title / artist / album / artwork URI).
     * This is what the system media notification and lock screen read — without it the card shows
     * no album photo and blank text, which is exactly the reported notification problem.
     */
    private fun QueueItem.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(songId.toString())
        .setUri(Uri.parse(filePath))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri?.let { Uri.parse(it) })
                .build()
        )
        .build()

    actual fun setQueue(items: List<QueueItem>, startIndex: Int) {
        val mediaItems = items.map { it.toMediaItem() }
        exoPlayer.setMediaItems(mediaItems, startIndex.coerceIn(0, mediaItems.size - 1), 0L)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    actual fun reorderQueue(items: List<QueueItem>, newIndex: Int) {
        if (items.isEmpty()) return
        val targetIds  = items.map { it.songId }
        val currentIds = (0 until exoPlayer.mediaItemCount)
            .map { exoPlayer.getMediaItemAt(it).mediaId?.toLongOrNull() }

        // Already exactly in the requested order -> nothing to do (zero window churn).
        if (currentIds == targetIds) return

        // Same set of tracks, only the ORDER changed (the normal sort case): reorder the live window
        // with moveMediaItem so the currently playing media source is NEVER re-created. setMediaItems
        // would rebuild the window and re-buffer the active item — the audible "kesedak" on sort.
        val sameSet = currentIds.size == targetIds.size &&
            currentIds.all { it != null } &&
            currentIds.toSet() == targetIds.toSet()
        if (sameSet) {
            val working = currentIds.toMutableList()
            for (i in targetIds.indices) {
                val want = targetIds[i]
                val pos  = working.indexOf(want)
                if (pos in 0 until working.size && pos != i) {
                    exoPlayer.moveMediaItem(pos, i)
                    val moved = working.removeAt(pos)
                    working.add(i, moved)
                }
            }
            return
        }

        // Different set (e.g. switched to/from "Favorit", or a search is active). Rebuilding the
        // window with setMediaItems mid-playback re-creates the active source and causes a hitch,
        // so while audio is playing we leave the live window untouched. The membership is picked up
        // on the next explicit play, or here once playback is paused/stopped (nothing to stutter).
        if (exoPlayer.isPlaying || exoPlayer.playWhenReady) return
        val mediaItems = items.map { it.toMediaItem() }
        exoPlayer.setMediaItems(
            mediaItems,
            newIndex.coerceIn(0, mediaItems.size - 1),
            exoPlayer.currentPosition
        )
    }

    actual fun playSong(item: QueueItem) {
        exoPlayer.setMediaItem(item.toMediaItem())
        exoPlayer.prepare()
        exoPlayer.play()
    }

    actual fun pause()  { exoPlayer.pause() }
    actual fun resume() { exoPlayer.play() }
    actual fun stop()   { exoPlayer.stop(); exoPlayer.seekTo(0) }

    actual fun seekTo(positionMs: Long) { exoPlayer.seekTo(positionMs) }
    // seekToNext/PreviousMediaItem honour the player's shuffle order and repeat mode set via
    // setPlayMode, so widget/app navigation follows the active playback mode automatically.
    actual fun skipNext()     { exoPlayer.seekToNextMediaItem(); if (exoPlayer.currentMediaItem != null) exoPlayer.play() }
    actual fun skipPrevious() { exoPlayer.seekToPreviousMediaItem(); if (exoPlayer.currentMediaItem != null) exoPlayer.play() }

    /** Media id of the item currently loaded in the player, i.e. the song's database id. */
    fun getCurrentMediaId(): Long? = exoPlayer.currentMediaItem?.mediaId?.toLongOrNull()

    /** Ordered song ids of the current playback window, used to rebuild the ViewModel queue on re-entry. */
    fun getQueueIds(): List<Long> =
        (0 until exoPlayer.mediaItemCount).mapNotNull { i ->
            exoPlayer.getMediaItemAt(i).mediaId?.toLongOrNull()
        }

    /** True when the player window already holds media. Detects the reclaimed/empty-player case
     *  where the UI still shows a track from persistence but there is nothing prepared to play. */
    fun hasMediaLoaded(): Boolean = exoPlayer.mediaItemCount > 0

    /**
     * Load a queue and prepare it, optionally starting playback. Used to re-arm playback after the
     * process was reclaimed (fresh, empty ExoPlayer window): the UI knows the last track from disk
     * but the player holds nothing, so transport buttons would only animate silently. Passing
     * autoplay=false leaves it prepared but paused (no unexpected audio blast when the app reopens).
     */
    fun prepareQueue(items: List<QueueItem>, startIndex: Int, autoplay: Boolean) {
        if (items.isEmpty()) return
        val mediaItems = items.map { it.toMediaItem() }
        exoPlayer.setMediaItems(mediaItems, startIndex.coerceIn(0, mediaItems.size - 1), 0L)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = autoplay
    }

    actual fun getCurrentQueueIndex(): Int = exoPlayer.currentMediaItemIndex

    actual fun setPlayMode(mode: PlayMode) {
        when (mode) {
            PlayMode.REPEAT_ONE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                exoPlayer.shuffleModeEnabled = false
            }
            PlayMode.SEQUENTIAL -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.shuffleModeEnabled = false
            }
            PlayMode.SHUFFLE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                exoPlayer.shuffleModeEnabled = true
            }
        }
    }

    actual fun getCurrentPosition(): Long = exoPlayer.currentPosition
    actual fun getDuration(): Long =
        exoPlayer.duration.takeIf { it != C.TIME_UNSET } ?: 0L

    actual fun isPlaying(): Boolean = exoPlayer.isPlaying

    /** Play intent (playWhenReady) — stable across buffering; use for button visuals. */
    actual fun isPlayIntent(): Boolean = exoPlayer.playWhenReady

    actual fun setPlaybackStateListener(listener: ((isPlaying: Boolean) -> Unit)?) {
        playbackListener = listener
    }

    actual fun setTrackChangeListener(listener: ((queueIndex: Int) -> Unit)?) {
        trackListener = listener
    }

    actual fun release() {
        if (wakeLock.isHeld) runCatching { wakeLock.release() }
        playbackListener = null
        trackListener = null
        mediaSession.release()
        exoPlayer.release()
    }
}
