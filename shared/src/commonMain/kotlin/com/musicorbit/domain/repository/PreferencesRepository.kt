package com.musicorbit.domain.repository

import com.musicorbit.domain.model.PlayMode
import com.musicorbit.domain.model.SortOrder
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    // ── Sort Order ──────────────────────────────────────────────────────────
    fun getSortOrder(): Flow<SortOrder>
    suspend fun saveSortOrder(order: SortOrder)

    // ── Play Mode ───────────────────────────────────────────────────────────
    fun getPlayMode(): Flow<PlayMode>
    suspend fun savePlayMode(mode: PlayMode)

    // ── Theme ───────────────────────────────────────────────────────────────
    // Values: "LIGHT" | "DARK" | "SYSTEM"
    fun getTheme(): Flow<String>
    suspend fun saveTheme(theme: String)

    // ── Last Played Song ────────────────────────────────────────────────────
    suspend fun getLastPlayedSongId(): Long?
    suspend fun saveLastPlayedSongId(id: Long)

    // ── Sleep Timer ─────────────────────────────────────────────────────────
    // Value in minutes; 0 = off
    fun getSleepTimerMinutes(): Flow<Int>
    suspend fun saveSleepTimerMinutes(minutes: Int)

    // ── Equalizer ───────────────────────────────────────────────────────────
    fun getEqualizerPreset(): Flow<String>
    suspend fun saveEqualizerPreset(preset: String)

    fun getEqualizerEnabled(): Flow<Boolean>
    suspend fun saveEqualizerEnabled(enabled: Boolean)

    // JSON array string e.g. "[0,-200,300,200,-100]"
    fun getEqualizerBands(): Flow<String>
    suspend fun saveEqualizerBands(bandsJson: String)
}
