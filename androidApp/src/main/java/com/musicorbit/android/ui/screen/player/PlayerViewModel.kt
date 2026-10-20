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
import kotlinx.coroutines.flow.collect
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

    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()

    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var queue: List<Song> = emptyList()

    /** Exposed for EqualizerViewModel to initialize the EQ with correct session ID. */
    val audioSessionId: Int
        get() = runCatching {
            audioPlayer.javaClass.getMethod("getAudioSessionId").invoke(audioPlayer) as? Int
        }.getOrNull() ?: 0

    init {
        restoreLastState()
        startPositionPolling()
        observeSleepTimer()
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
                val idx = audioPlayer.getCurrentQueueIndex()
                _playbackState.update { state ->
                    state.copy(
                        isPlaying       = audioPlayer.isPlaying(),
                        currentPosition = audioPlayer.getCurrentPosition(),
                        duration        = audioPlayer.getDuration(),
                        currentSong     = if (idx >= 0 && idx < queue.size) queue[idx] else state.currentSong
                    )
                }
                delay(500L)
            }
        }
    }

    /**
     * Play a song from a given list (queue). Sets the full queue on ExoPlayer
     * so that skip next/previous work correctly.
     */
    fun playFromQueue(songs: List<Song>, startIndex: Int) {
        queue = songs
        val items = songs.map { it.filePath to it.id }
        audioPlayer.setQueue(items, startIndex)
        _playbackState.update { it.copy(currentSong = songs[startIndex], isPlaying = true) }
        viewModelScope.launch {
            songRepository.incrementPlayCount(songs[startIndex].id)
            preferencesRepository.saveLastPlayedSongId(songs[startIndex].id)
        }
    }

    fun playSong(song: Song) {
        audioPlayer.playSong(song.filePath, song.id)
        queue = listOf(song)
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

    fun skipNext() {
        audioPlayer.skipNext()
        val idx = audioPlayer.getCurrentQueueIndex()
        if (idx >= 0 && idx < queue.size) {
            _playbackState.update { it.copy(currentSong = queue[idx]) }
            viewModelScope.launch {
                songRepository.incrementPlayCount(queue[idx].id)
                preferencesRepository.saveLastPlayedSongId(queue[idx].id)
            }
        }
    }

    fun skipPrevious() {
        audioPlayer.skipPrevious()
        val idx = audioPlayer.getCurrentQueueIndex()
        if (idx >= 0 && idx < queue.size) {
            _playbackState.update { it.copy(currentSong = queue[idx]) }
            viewModelScope.launch {
                songRepository.incrementPlayCount(queue[idx].id)
                preferencesRepository.saveLastPlayedSongId(queue[idx].id)
            }
        }
    }

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

    // ── Sleep Timer ──────────────────────────────────────────────────────────

    /**
     * Observe the saved sleep timer minutes.
     * When a non-zero value is set (or changed), restart the countdown.
     * When set to 0, cancel the timer.
     */
    private fun observeSleepTimer() {
        viewModelScope.launch {
            preferencesRepository.getSleepTimerMinutes().collect { minutes ->
                sleepTimerJob?.cancel()
                sleepTimerJob = null
                _sleepTimerRemaining.value = null

                if (minutes > 0) {
                    startSleepCountdown(minutes * 60_000L)
                }
            }
        }
    }

    private fun startSleepCountdown(durationMs: Long) {
        sleepTimerJob = viewModelScope.launch {
            var remaining = durationMs
            _sleepTimerRemaining.value = remaining
            while (remaining > 0) {
                delay(1_000L)
                remaining -= 1_000L
                _sleepTimerRemaining.value = remaining
            }
            // Timer expired — stop playback
            _sleepTimerRemaining.value = null
            audioPlayer.pause()
            _playbackState.update { it.copy(isPlaying = false) }
            // Reset preference so timer doesn't restart on next launch
            preferencesRepository.saveSleepTimerMinutes(0)
        }
    }

    /** Allow UI to cancel the timer (e.g. button "Batalkan timer"). */
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemaining.value = null
        viewModelScope.launch { preferencesRepository.saveSleepTimerMinutes(0) }
    }

    override fun onCleared() {
        positionJob?.cancel()
        sleepTimerJob?.cancel()
        // NOTE: we intentionally do NOT call audioPlayer.release() here.
        // The ExoPlayer + MediaSession are owned by the AudioPlayer Koin singleton and are
        // shared with MusicService so playback can continue in the background. Releasing them
        // when this ViewModel clears would kill background audio.
        super.onCleared()
    }
}
