package com.musicorbit.platform

import com.musicorbit.domain.model.SongData

/**
 * Platform-specific music file scanner.
 * Android actual: MediaStore + MediaMetadataRetriever (ID3 tags)
 * iOS actual:     UIDocumentPickerViewController (stub — user adds files manually)
 */
expect class MusicScanner {
    suspend fun scanAudioFiles(): List<SongData>
}
