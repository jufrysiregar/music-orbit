package com.musicorbit.android.widget

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current "now playing" snapshot so the home-screen Glance widget can render
 * the track title, artist and play/pause state without depending on a ViewModel lifecycle.
 *
 * Registered as a Koin `single` at application scope, so it lives as long as the process —
 * which is kept alive by the Media3 foreground [com.musicorbit.android.service.MusicService]
 * whenever audio is playing.
 */
class NowPlayingStore(private val context: Context) {

    data class NowPlaying(
        val title: String = "",
        val artist: String = "",
        val isPlaying: Boolean = false,
        val hasSong: Boolean = false
    )

    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state.asStateFlow()

    /**
     * Publish a new snapshot. When the visible fields actually change we refresh every
     * attached widget instance so the home screen stays in sync with playback.
     */
    fun update(title: String, artist: String, isPlaying: Boolean, hasSong: Boolean) {
        val current = _state.value
        val next = NowPlaying(
            title     = title,
            artist    = artist,
            isPlaying = isPlaying,
            hasSong   = hasSong
        )
        if (current != next) {
            _state.value = next
            MusicWidget().updateAll(context)
        }
    }

    /**
     * Update only the play state, keeping the current title / artist. Used right after a
     * widget transport tap so the icon flips instantly even if the UI ViewModel is not polling.
     */
    fun setPlaying(isPlaying: Boolean) {
        val current = _state.value
        if (current.isPlaying != isPlaying) {
            _state.value = current.copy(isPlaying = isPlaying)
            MusicWidget().updateAll(context)
        }
    }
}
