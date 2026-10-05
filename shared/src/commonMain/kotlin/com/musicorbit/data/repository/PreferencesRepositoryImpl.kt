package com.musicorbit.data.repository

import com.musicorbit.domain.model.PlayMode
import com.musicorbit.domain.model.SortOrder
import com.musicorbit.domain.repository.PreferencesRepository
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getBooleanFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private object Keys {
    const val LAST_PLAYED_SONG_ID = "last_played_song_id"
    const val SORT_ORDER          = "sort_order"
    const val PLAY_MODE           = "play_mode"
    const val THEME               = "theme"
    const val LANGUAGE            = "language"
    const val SLEEP_TIMER_MINUTES = "sleep_timer"
    const val EQUALIZER_PRESET    = "eq_preset"
    const val EQUALIZER_BANDS     = "eq_bands"
    const val EQUALIZER_ENABLED   = "eq_enabled"
}

@OptIn(ExperimentalSettingsApi::class)
class PreferencesRepositoryImpl(
    private val settings: ObservableSettings
) : PreferencesRepository {

    // ── Sort Order ─────────────────────────────────────────────────────────

    override fun getSortOrder(): Flow<SortOrder> =
        settings.getStringFlow(Keys.SORT_ORDER, SortOrder.DATE_ADDED.name)
            .map { runCatching { SortOrder.valueOf(it) }.getOrDefault(SortOrder.DATE_ADDED) }

    override suspend fun saveSortOrder(order: SortOrder) {
        settings.putString(Keys.SORT_ORDER, order.name)
    }

    // ── Play Mode ──────────────────────────────────────────────────────────

    override fun getPlayMode(): Flow<PlayMode> =
        settings.getStringFlow(Keys.PLAY_MODE, PlayMode.SEQUENTIAL.name)
            .map { runCatching { PlayMode.valueOf(it) }.getOrDefault(PlayMode.SEQUENTIAL) }

    override suspend fun savePlayMode(mode: PlayMode) {
        settings.putString(Keys.PLAY_MODE, mode.name)
    }

    // ── Theme ──────────────────────────────────────────────────────────────

    override fun getTheme(): Flow<String> =
        settings.getStringFlow(Keys.THEME, "SYSTEM")

    override suspend fun saveTheme(theme: String) {
        settings.putString(Keys.THEME, theme)
    }

    // ── Last Played Song ───────────────────────────────────────────────────

    override suspend fun getLastPlayedSongId(): Long? {
        val value = settings.getLongOrNull(Keys.LAST_PLAYED_SONG_ID)
        return if (value != null && value > 0L) value else null
    }

    override suspend fun saveLastPlayedSongId(id: Long) {
        settings.putLong(Keys.LAST_PLAYED_SONG_ID, id)
    }

    // ── Sleep Timer ────────────────────────────────────────────────────────

    override fun getSleepTimerMinutes(): Flow<Int> =
        settings.getIntFlow(Keys.SLEEP_TIMER_MINUTES, 0)

    override suspend fun saveSleepTimerMinutes(minutes: Int) {
        settings.putInt(Keys.SLEEP_TIMER_MINUTES, minutes)
    }

    // ── Equalizer Preset ───────────────────────────────────────────────────

    override fun getEqualizerPreset(): Flow<String> =
        settings.getStringFlow(Keys.EQUALIZER_PRESET, "Normal")

    override suspend fun saveEqualizerPreset(preset: String) {
        settings.putString(Keys.EQUALIZER_PRESET, preset)
    }

    // ── Equalizer Enabled ──────────────────────────────────────────────────

    override fun getEqualizerEnabled(): Flow<Boolean> =
        settings.getBooleanFlow(Keys.EQUALIZER_ENABLED, false)

    override suspend fun saveEqualizerEnabled(enabled: Boolean) {
        settings.putBoolean(Keys.EQUALIZER_ENABLED, enabled)
    }

    // ── Equalizer Bands ────────────────────────────────────────────────────

    override fun getEqualizerBands(): Flow<String> =
        settings.getStringFlow(Keys.EQUALIZER_BANDS, "[]")

    override suspend fun saveEqualizerBands(bandsJson: String) {
        settings.putString(Keys.EQUALIZER_BANDS, bandsJson)
    }
}
