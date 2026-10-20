package com.musicorbit.platform

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.musicorbit.domain.model.Lyrics
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

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        val selection     = "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val selectionArgs = arrayOf("10000")
        val sortOrder     = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
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
                val realPath  = if (dataCol >= 0) cursor.getString(dataCol) else null

                val rawTitle  = cursor.getString(titleCol)?.trim()
                val rawArtist = cursor.getString(artistCol)?.trim()
                val rawAlbum  = cursor.getString(albumCol)?.trim()

                val title  = rawTitle?.takeIf  { it.isNotBlank() && it != "<unknown>" }
                    ?: fileNameFallback(realPath.orEmpty(), id)
                val artist = rawArtist?.takeIf { it.isNotBlank() && it != "<unknown>" }
                    ?: tryId3Artist(realPath.orEmpty())
                val album  = rawAlbum?.takeIf  { it.isNotBlank() && it != "<unknown>" }
                    ?: "Album tidak diketahui"

                val contentUri  = ContentUris.withAppendedId(collection, id).toString()
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                ).toString()

                results.add(
                    SongData(
                        id           = id,
                        title        = title,
                        artist       = artist,
                        album        = album,
                        duration     = duration,
                        filePath     = contentUri,
                        albumArtUri  = albumArtUri,
                        dateAdded    = dateAdded,
                        realFilePath = realPath?.takeIf { it.isNotBlank() }
                    )
                )
            }
        }
        results
    }

    /**
     * Look for sibling .lrc or .txt files matching each song's real file path.
     * Priority: .lrc > .txt. Returns one [Lyrics] entry for each match.
     */
    actual suspend fun scanLyricsForSongs(songs: List<SongData>): List<Lyrics> =
        withContext(Dispatchers.IO) {
            songs.mapNotNull { song ->
                val realPath = song.realFilePath ?: return@mapNotNull null
                findLyricsForFile(song.id, realPath)
            }
        }

    private fun findLyricsForFile(songId: Long, audioFilePath: String): Lyrics? {
        val audioFile = try { File(audioFilePath) } catch (_: Exception) { return null }
        if (!audioFile.exists()) return null

        val baseName = audioFile.nameWithoutExtension
        val parent   = audioFile.parentFile ?: return null

        fun tryRead(file: File, isLrc: Boolean): Lyrics? = try {
            if (!file.exists() || !file.canRead()) return null
            val raw  = file.readText().trim()
            val text = if (isLrc) stripLrcTimestamps(raw) else raw
            if (text.isNotBlank()) Lyrics(songId = songId, lyricsText = text, hasLrc = isLrc) else null
        } catch (_: Exception) { null }  // may fail on Android 10+ scoped storage

        // 1. Try .lrc (with timestamps)
        tryRead(File(parent, "$baseName.lrc"), true)?.let { return it }
        // 2. Try case-insensitive .LRC
        tryRead(File(parent, "$baseName.LRC"), true)?.let { return it }
        // 3. Try .txt
        tryRead(File(parent, "$baseName.txt"), false)?.let { return it }
        // 4. Try .TXT
        tryRead(File(parent, "$baseName.TXT"), false)?.let { return it }

        // 5. Try "lyrics.lrc" / "Lyrics.txt" in the same folder (common for albums)
        listOf("lyrics.lrc" to true, "Lyrics.lrc" to true, "lyrics.txt" to false, "Lyrics.txt" to false)
            .forEach { (name, isLrc) -> tryRead(File(parent, name), isLrc)?.let { return it } }

        return null
    }

    /**
     * Remove LRC timestamp markers like [00:01.23] and metadata lines like [ar:Artist]
     * from a .lrc file. Returns plain lyric text ready for display.
     */
    private fun stripLrcTimestamps(text: String): String {
        val lrcLine = Regex("""\[\d{2}:\d{2}[.:]\d{2,3}](.*)""")
        val lines = text.lines().mapNotNull { line ->
            val match = lrcLine.find(line)
            if (match != null) {
                // Timestamped line — extract lyric text after the timestamp
                match.groupValues[1].trim().ifBlank { null }
            } else if (line.startsWith("[")) {
                // Metadata line (e.g. [ar:Artist], [al:Album]) — skip
                null
            } else {
                // Plain line without timestamp (some LRC files have this)
                line.trim().ifBlank { null }
            }
        }
        return if (lines.isEmpty()) text else lines.joinToString("\n")
    }

    /**
     * Fallback title from filename when MediaStore has no metadata.
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
     * Last-resort ID3 artist read via MediaMetadataRetriever.
     * Uses try/finally for compatibility with Android 7–9 (minSdk 24).
     */
    private fun tryId3Artist(filePath: String): String {
        if (filePath.isBlank() || filePath.startsWith("content://")) {
            return "Artis tidak diketahui"
        }
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(filePath)
            val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            artist?.trim()?.takeIf { it.isNotBlank() && it != "<unknown>" }
                ?: "Artis tidak diketahui"
        } catch (_: Exception) {
            "Artis tidak diketahui"
        } finally {
            try { mmr.release() } catch (_: Exception) {}
        }
    }

    internal fun extractId3Metadata(filePath: String): Triple<String, String, String> {
        val title  = fileNameFallback(filePath, 0)
        val artist = tryId3Artist(filePath)
        return Triple(title, artist, "Album tidak diketahui")
    }
}
