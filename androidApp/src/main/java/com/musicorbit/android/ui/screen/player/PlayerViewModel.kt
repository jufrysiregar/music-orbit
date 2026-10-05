package com.musicorbit.android.ui.screen.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicorbit.domain.model.PlayMode
import com.musicorbit.domain.model.PlaybackState
import com.musicorbit.domain.model.Song
import com.musicorbit.domain.repository.PreferencesRepository
import com.musicorbit.domain.repository.SongRepository
import com.musicorbit.platform.AudioPlayer
import com.musicorbit.platform.EqualizerEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val audioPlayer: AudioPlayer,
    private val songRepository: SongRepository,
    private val preferencesRepository: PreferencesRepository,
    private val equalizerEngine: EqualizerEngine
) : ViewModel() {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var positionJob: Job? = null

    /** Exposed for EqualizerViewModel to initialize the EQ with correct session ID. */
    val audioSessionId: Int
        get() = (audioPlayer as? com.musicorbit.platform.AudioPlayer)
            .let {
                // Reflectively access audioSessionId only on Android actual
                runCatching {
                    it?.javaClass?.getMethod("getAudioSessionId")?.invoke(it) as? Int ?: 0
                }.getOrDefault(0)
            }

    init {
        restoreLastState()
        startPositionPolling()
    }

    private fun restoreLastState() {
        viewModelScope.launch {
            val lastId = preferencesRepository.getLastPlayedSongId()
            if (lastId != null) {
                songRepository.getSongById(lastId)?.let { song ->
                    _playbackState.update { it.copy(currentSong = song) }
                }
            }
            val savedMode = preferencesRepository.getPlayMode().first()
            audioPlayer.setPlayMode(savedMode)
            _playbackState.update { it.copy(playMode = savedMode) }
        }
    }

    private fun startPositionPolling() {
        positionJob = viewModelScope.launch {
            while (true) {
                _playbackState.update { state ->
                    state.copy(
                        isPlaying       = audioPlayer.isPlaying(),
                        currentPosition = audioPlayer.getCurrentPosition(),
                        duration        = audioPlayer.getDuration()
                    )
                }
                delay(500L)
            }
        }
    }

    fun playSong(song: Song) {
        audioPlayer.playSong(song.filePath, song.id)
        _playbackState.update { it.copy(currentSong = song, isPlaying = true) }
        viewModelScope.launch {
            songRepository.incrementPlayCount(song.id)
            preferencesRepository.saveLastPlayedSongId(song.id)
        }
    }

    fun togglePlayPause() {
        if (audioPlayer.isPlaying()) audioPlayer.pause() else audioPlayer.resume()
        _playbackState.update { it.copy(isPlaying = audioPlayer.isPlaying()) }
    }

    fun stop() {
        audioPlayer.stop()
        _playbackState.update { it.copy(isPlaying = false, currentPosition = 0L) }
    }

    fun seekTo(positionMs: Long) { audioPlayer.seekTo(positionMs) }
    fun skipNext()     { audioPlayer.skipNext() }
    fun skipPrevious() { audioPlayer.skipPrevious() }

    /** Cycles: SEQUENTIAL → SHUFFLE → REPEAT_ONE → SEQUENTIAL */
    fun cyclePlayMode() {
        val next = when (_playbackState.value.playMode) {
            PlayMode.SEQUENTIAL -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE    -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.SEQUENTIAL
        }
        audioPlayer.setPlayMode(next)
        _playbackState.update { it.copy(playMode = next) }
        viewModelScope.launch { preferencesRepository.savePlayMode(next) }
    }

    /** Update song title/artist directly in DB (used by Edit dialog on MainPlayerScreen). */
    fun updateSongMetadata(song: Song) {
        viewModelScope.launch {
            songRepository.upsertSong(song)
            _playbackState.update { it.copy(currentSong = song) }
        }
    }

    override fun onCleared() {
        positionJob?.cancel()
        audioPlayer.release()
        super.onCleared()
    }
}
