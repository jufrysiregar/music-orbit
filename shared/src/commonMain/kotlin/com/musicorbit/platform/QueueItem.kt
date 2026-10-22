package com.musicorbit.platform

import com.musicorbit.domain.model.Song

/**
 * A single playable queue entry that carries everything the platform player needs to both play
 * audio and drive rich system UI (media notification artwork, lock-screen / Bluetooth metadata).
 *
 * Android maps this onto a Media3 [androidx.media3.common.MediaItem] whose [MediaMetadata] holds
 * the title / artist / album / artwork URI — that is what makes the notification show the album
 * photo and transport buttons. iOS ignores the extra fields (stub). Keeping the shape shared lets
 * the common [AudioPlayer] API stay platform-agnostic instead of passing bare file/id pairs.
 */
data class QueueItem(
    val filePath: String,
    val songId: Long,
    val title: String = "",
    val artist: String = "",
    val album: String? = null,
    val artworkUri: String? = null
)

/**
 * Map a domain [Song] to a platform [QueueItem] carrying the metadata the media notification and
 * lock screen need (album art / title / artist). Shared so the player UI and the home-screen widget
 * build queue entries identically.
 */
fun Song.toQueueItem(): QueueItem = QueueItem(
    filePath   = filePath,
    songId     = id,
    title      = title,
    artist     = artist,
    album      = album,
    artworkUri = albumArtUri
)
