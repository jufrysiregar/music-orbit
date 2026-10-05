package com.musicorbit.domain.model

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,   // milliseconds
    val duration: Long = 0L,          // milliseconds
    val playMode: PlayMode = PlayMode.SEQUENTIAL
)
