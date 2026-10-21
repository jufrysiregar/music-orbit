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
import com.musicorbit.android.widget.NowPlayingStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val audioPlayer: AudioPlayer,
    private val songRepository: SongRepository,
    private val preferencesRepository: PreferencesRepository,
    private val equalizerEngine: EqualizerEngine,
    private val nowPlayingStore: NowPlayingStore
) : ViewModel() {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()

    /** Set of favorite song ids, observed live so the heart icon and list stay in sync. */
    val favorites: StateFlow<Set<Long>> = preferencesRepository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var queue: List<Song> = emptyList()
    private var lastRecordedId: Long? = null

    /**
     * Records a track as played exactly once. Both the explicit actions and the player's
     * onMediaItemTransition callback route through here, so the id guard prevents double-counting
     * when a single track change triggers both paths.
     */
    private fun recordPlayback(song: Song) {
        if (song.id == lastRecordedId) return
        lastRecordedId = song.id
        viewModelScope.launch {
            songRepository.incrementPlayCount(song.id)
            preferencesRepository.saveLastPlayedSongId(song.id)
        }
    }

    /** Exposed for EqualizerViewModel to initialize the EQ with correct session ID. */
    val audioSessionId: Int
        get() = runCatching {
            audioPlayer.javaClass.getMethod("getAudioSessionId").invoke(audioPlayer) as? Int
        }.getOrNull() ?: 0

    init {
        restoreLastState()
        bindPlaybackListener()
        startPositionPolling()
        observeSleepTimer()
    }

    /**
     * The player pushes its own play/pause transitions, so the UI and the home-screen widget stay
     * in sync immediately (and survive long idle periods) instead of relying on polling alone.
     */
    private fun bindPlaybackListener() {
        audioPlayer.setPlaybackStateListener { isPlaying ->
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            nowPlayingStore.setPlaying(isPlaying)
        }
        // Immediate track-change sync: when Next/Prev/auto-advance fires (including from the
        // notification or home-screen widget), refresh title/artist/art in the footer & player
        // without waiting for the next poll tick.
        audioPlayer.setTrackChangeListener { index ->
            val song = queue.getOrNull(index)
            if (song != null) {
                _playbackState.update { it.copy(currentSong = song) }
                publishNowPlaying()
                recordPlayback(song)
            }
        }
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
                publishNowPlaying()
                delay(250L)
            }
        }
    }

    /** Mirror the current track + play state to the widget store. */
    private fun publishNowPlaying() {
        val song = _playbackState.value.currentSong
        nowPlayingStore.update(
            title       = song?.title ?: "",
            artist      = song?.artist ?: "",
            isPlaying   = _playbackState.value.isPlaying,
            hasSong     = song != null,
            albumArtUri = song?.albumArtUri
        )
    }

    /**
     * Play a song from a given list (queue). Sets the full queue on ExoPlayer
     * so that skip next/previous work correctly.
     */
    fun playFromQueue(songs: List<Song>, startIndex: Int) {
        queue = songs
        val items = songs.map { it.filePath to it.id }
        audioPlayer.setQueue(items, startIndex)
        // Re-apply the active play mode so shuffle / repeat survive a fresh queue.
        audioPlayer.setPlayMode(_playbackState.value.playMode)
        _playbackState.update { it.copy(currentSong = songs[startIndex], isPlaying = true) }
        recordPlayback(songs[startIndex])
    }

    /**
     * Re-align the live playback queue to a newly sorted list (e.g. after the header sort order
     * changes), keeping the current song and its position and preserving the play/pause state.
     * This is what makes "Berurut" (Sequential) follow the header's "urutkan berdasarkan" choice.
     */
    fun resyncQueue(songs: List<Song>) {
        val currentId = _playbackState.value.currentSong?.id ?: return
        val newIndex  = songs.indexOfFirst { it.id == currentId }
        if (newIndex < 0) return

        val wasPlaying = _playbackState.value.isPlaying
        val position   = audioPlayer.getCurrentPosition()
        val items      = songs.map { it.filePath to it.id }

        audioPlayer.setQueue(items, newIndex)
        audioPlayer.setPlayMode(_playbackState.value.playMode)
        if (position > 0) audioPlayer.seekTo(position)
        // setQueue() auto-plays; restore the previous paused state if we weren't playing.
        if (!wasPlaying) audioPlayer.pause()

        queue = songs
        _playbackState.update { it.copy(currentSong = songs[newIndex]) }
    }

    fun playSong(song: Song) {
        audioPlayer.playSong(song.filePath, song.id)
        queue = listOf(song)
        _playbackState.update { it.copy(currentSong = song, isPlaying = true) }
        recordPlayback(song)
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

    /** Toggle the favorite state of a song, persisted via preferences. */
    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { preferencesRepository.toggleFavorite(songId) }
    }

    fun skipNext() {
        audioPlayer.skipNext()
        val idx = audioPlayer.getCurrentQueueIndex()
        if (idx >= 0 && idx < queue.size) {
            _playbackState.update { it.copy(currentSong = queue[idx]) }
            recordPlayback(queue[idx])
        }
    }

    fun skipPrevious() {
        audioPlayer.skipPrevious()
        val idx = audioPlayer.getCurrentQueueIndex()
        if (idx >= 0 && idx < queue.size) {
            _playbackState.update { it.copy(currentSong = queue[idx]) }
            recordPlayback(queue[idx])
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
            // Keep the in-memory queue in sync: the 250ms poll re-reads queue[idx] and would
            // otherwise revert the edited title/artist. Also refresh Now Playing + the widget now.
            queue = queue.map { if (it.id == song.id) song else it }
            _playbackState.update { st -> if (st.currentSong?.id == song.id) st.copy(currentSong = song) else st }
            publishNowPlaying()
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
        // Detach the listener so a cleared ViewModel doesn't keep receiving player callbacks.
        audioPlayer.setPlaybackStateListener(null)
        audioPlayer.setTrackChangeListener(null)
        // NOTE: we intentionally do NOT call audioPlayer.release() here.
        // The ExoPlayer + MediaSession are owned by the AudioPlayer Koin singleton and are
        // shared with MusicService so playback can continue in the background. Releasing them
        // when this ViewModel clears would kill background audio.
        super.onCleared()
    }
}
