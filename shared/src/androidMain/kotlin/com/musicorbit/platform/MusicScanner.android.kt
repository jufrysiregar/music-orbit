package com.musicorbit.platform

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.musicorbit.domain.model.SongData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class MusicScanner(private val context: Context) {

    private val SUPPORTED_MIME = setOf(
        "audio/mpeg",   // .mp3
        "audio/flac",   // .flac
        "audio/aac",    // .aac
        "audio/ogg",    // .ogg
        "audio/mp4"     // .m4a
    )

    actual suspend fun scanAudioFiles(): List<SongData> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SongData>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        context.contentResolver.query(
            uri,
            projection,
            "${MediaStore.Audio.Media.DURATION} >= ?",
            arrayOf("10000"),   // minimum 10 seconds
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val mimeCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val albumIdCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeCol) ?: continue
                if (mime !in SUPPORTED_MIME) continue

                val id        = cursor.getLong(idCol)
                val filePath  = cursor.getString(dataCol) ?: continue
                val duration  = cursor.getLong(durationCol)
                val dateAdded = cursor.getLong(dateCol) * 1000L
                val albumId   = cursor.getLong(albumIdCol)

                val (title, artist, album) = extractId3Metadata(filePath)
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                ).toString()

                results.add(SongData(id, title, artist, album, duration, filePath, albumArtUri, dateAdded))
            }
        }
        results
    }

    /**
     * Extracts ID3 tags via MediaMetadataRetriever.
     * Priority: ID3 tag → fallback (replace _ and - with spaces in filename).
     * Ensures title and artist are NEVER empty strings (Requirement 1.3).
     */
    internal fun extractId3Metadata(filePath: String): Triple<String, String, String> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)

            val rawTitle  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            val rawAlbum  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

            val fileName = File(filePath).nameWithoutExtension
            val fallback = fileName.replace('_', ' ').replace('-', ' ').trim()
                .ifBlank { "Lagu tidak diketahui" }

            Triple(
                rawTitle?.takeIf  { it.isNotBlank() } ?: fallback,
                rawArtist?.takeIf { it.isNotBlank() } ?: "Artis tidak diketahui",
                rawAlbum?.takeIf  { it.isNotBlank() } ?: "Album tidak diketahui"
            )
        } catch (e: Exception) {
            val fallback = runCatching {
                File(filePath).nameWithoutExtension.replace('_', ' ').replace('-', ' ').trim()
            }.getOrDefault("Lagu tidak diketahui")
            Triple(fallback, "Artis tidak diketahui", "Album tidak diketahui")
        } finally {
            runCatching { retriever.release() }
        }
    }
}
