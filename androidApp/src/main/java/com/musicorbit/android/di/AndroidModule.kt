package com.musicorbit.android.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.musicorbit.db.MusicOrbitDatabase
import com.musicorbit.platform.AudioPlayer
import com.musicorbit.platform.EqualizerEngine
import com.musicorbit.platform.MusicScanner
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.musicorbit.android.ui.screen.player.PlayerViewModel
import com.musicorbit.android.ui.screen.home.HomeViewModel
import com.musicorbit.android.ui.screen.lyrics.LyricsViewModel
import com.musicorbit.android.ui.screen.equalizer.EqualizerViewModel
import com.musicorbit.android.ui.screen.settings.SettingsViewModel

val androidModule = module {

    // ── SQLDelight driver ───────────────────────────────────────────────────
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema  = MusicOrbitDatabase.Schema,
            context = androidContext(),
            name    = "musicorbit.db"
        )
    }

    // ── MultiplatformSettings (backed by SharedPreferences) ─────────────────
    single<ObservableSettings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("music_orbit_prefs", Context.MODE_PRIVATE)
        )
    }

    // ── Platform actual implementations ─────────────────────────────────────
    single { MusicScanner(androidContext()) }
    single { AudioPlayer(androidContext()) }
    single { EqualizerEngine() }

    // ── ViewModels ──────────────────────────────────────────────────────────
    viewModel { PlayerViewModel(get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { LyricsViewModel(get(), get(), get()) }
    viewModel { EqualizerViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
