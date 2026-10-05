package com.musicorbit.platform

import com.musicorbit.domain.model.SongData

/**
 * iOS stub — user adds files manually via UIDocumentPickerViewController.
 * No auto-scan equivalent to MediaStore exists on iOS.
 * Full implementation to be added in a future sprint.
 */
actual class MusicScanner {
    actual suspend fun scanAudioFiles(): List<SongData> = emptyList() // TODO: UIDocumentPicker
}
