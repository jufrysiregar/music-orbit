package com.musicorbit.platform

import com.musicorbit.domain.model.Lyrics
import com.musicorbit.domain.model.SongData

/**
 * Platform-specific music file scanner.
 * Android actual: MediaStore + MediaMetadataRetriever (ID3 tags) + .lrc/.txt file reader
 * iOS actual:     UIDocumentPickerViewController (stub — user adds files manually)
 */
expect class MusicScanner {
    suspend fun scanAudioFiles(): List<SongData>

    /**
     * Scan for sibling lyrics files (.lrc / .txt) for each song in [songs].
     * Returns one [Lyrics] entry per song that has a matching file.
     */
    suspend fun scanLyricsForSongs(songs: List<SongData>): List<Lyrics>
}
