package com.musicorbit.di

import com.musicorbit.data.repository.LyricsRepositoryImpl
import com.musicorbit.data.repository.PreferencesRepositoryImpl
import com.musicorbit.data.repository.SongRepositoryImpl
import com.musicorbit.db.MusicOrbitDatabase
import com.musicorbit.domain.repository.LyricsRepository
import com.musicorbit.domain.repository.PreferencesRepository
import com.musicorbit.domain.repository.SongRepository
import com.musicorbit.domain.usecase.GetLyricsUseCase
import com.musicorbit.domain.usecase.GetSongsUseCase
import com.musicorbit.domain.usecase.GetSortedSongsUseCase
import com.musicorbit.domain.usecase.SaveLyricsUseCase
import com.musicorbit.domain.usecase.ScanMusicUseCase
import org.koin.dsl.module

/**
 * Koin module for all shared (commonMain) dependencies.
 * Platform-specific bindings (SqlDriver, AudioPlayer, MusicScanner,
 * EqualizerEngine, ViewModels) are provided in each platform's own module.
 */
val sharedModule = module {

    // ── Database ────────────────────────────────────────────────────────────
    // SqlDriver is provided by the platform module (androidModule / iosModule)
    single { MusicOrbitDatabase(get()) }

    // ── Repositories ────────────────────────────────────────────────────────
    single<SongRepository>        { SongRepositoryImpl(get()) }
    single<LyricsRepository>      { LyricsRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }

    // ── Use Cases ────────────────────────────────────────────────────────────
    factory { GetSongsUseCase(get()) }
    factory { GetSortedSongsUseCase(get()) }
    factory { ScanMusicUseCase(get(), get()) }   // SongRepository + MusicScanner
    factory { SaveLyricsUseCase(get()) }
    factory { GetLyricsUseCase(get()) }
}
