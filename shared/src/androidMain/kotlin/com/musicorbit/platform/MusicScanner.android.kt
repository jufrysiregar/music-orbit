package com.musicorbit.platform

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.musicorbit.domain.model.SongData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class MusicScanner(private val context: Context) {

    actual suspend fun scanAudioFiles(): List<SongData> = withContext(Dispatchers.IO) {
        val results    = mutableListOf<SongData>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        // Include TITLE, ARTIST, ALBUM directly from MediaStore — faster and
        // works without file-path access on Android 10+ scoped storage.
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA          // still useful as fallback on older APIs
        )

        val selection     = "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val selectionArgs = arrayOf("10000")     // ≥ 10 seconds
        val sortOrder     = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataCol     = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id        = cursor.getLong(idCol)
                val duration  = cursor.getLong(durationCol)
                val dateAdded = cursor.getLong(dateCol) * 1_000L
                val albumId   = cursor.getLong(albumIdCol)
                val filePath  = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""

                // MediaStore title/artist/album — already cleaned by the OS
                val rawTitle  = cursor.getString(titleCol)?.trim()
                val rawArtist = cursor.getString(artistCol)?.trim()
                val rawAlbum  = cursor.getString(albumCol)?.trim()

                // Sanitise: strip Android's "<unknown>" placeholder
                val title  = rawTitle?.takeIf   { it.isNotBlank() && it != "<unknown>" }
                    ?: fileNameFallback(filePath, id)
                val artist = rawArtist?.takeIf  { it.isNotBlank() && it != "<unknown>" }
                    ?: tryId3Artist(filePath)
                val album  = rawAlbum?.takeIf   { it.isNotBlank() && it != "<unknown>" }
                    ?: "Album tidak diketahui"

                // Content URI used for streaming — works on all Android versions
                val contentUri  = ContentUris.withAppendedId(collection, id).toString()
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                ).toString()

                results.add(
                    SongData(
                        id          = id,
                        title       = title,
                        artist      = artist,
                        album       = album,
                        duration    = duration,
                        filePath    = contentUri,   // use content URI, not file path
                        albumArtUri = albumArtUri,
                        dateAdded   = dateAdded
                    )
                )
            }
        }
        results
    }

    /**
     * Fallback title from filename when MediaStore has no metadata.
     * Replaces underscores and hyphens with spaces, trims, title-cases.
     */
    private fun fileNameFallback(filePath: String, id: Long): String {
        if (filePath.isBlank()) return "Lagu $id"
        return File(filePath).nameWithoutExtension
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifBlank { "Lagu $id" }
    }

    /**
     * Last-resort ID3 artist read via MediaMetadataRetriever for files
     * where MediaStore reports "<unknown>" but the file has embedded tags.
     * Only attempted when filePath is a real file path (not a content URI).
     */
    private fun tryId3Artist(filePath: String): String {
        if (filePath.isBlank() || filePath.startsWith("content://")) {
            return "Artis tidak diketahui"
        }
        return try {
            MediaMetadataRetriever().use { mmr ->
                mmr.setDataSource(filePath)
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                artist?.trim()?.takeIf { it.isNotBlank() && it != "<unknown>" }
                    ?: "Artis tidak diketahui"
            }
        } catch (_: Exception) {
            "Artis tidak diketahui"
        }
    }

    // Keep for unit-test compatibility
    internal fun extractId3Metadata(filePath: String): Triple<String, String, String> {
        val title  = fileNameFallback(filePath, 0)
        val artist = tryId3Artist(filePath)
        return Triple(title, artist, "Album tidak diketahui")
    }
}
