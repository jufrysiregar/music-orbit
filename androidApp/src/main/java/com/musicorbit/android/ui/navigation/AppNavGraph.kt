package com.musicorbit.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.musicorbit.android.ui.screen.about.AboutScreen
import com.musicorbit.android.ui.screen.equalizer.EqualizerScreen
import com.musicorbit.android.ui.screen.equalizer.EqualizerViewModel
import com.musicorbit.android.ui.screen.home.HomeScreen
import com.musicorbit.android.ui.screen.home.HomeViewModel
import com.musicorbit.android.ui.screen.lyrics.LyricsScreen
import com.musicorbit.android.ui.screen.player.MainPlayerScreen
import com.musicorbit.android.ui.screen.player.PlayerViewModel
import com.musicorbit.android.ui.screen.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavGraph() {
    val navController   = rememberNavController()
    val playerViewModel = koinViewModel<PlayerViewModel>()
    val homeViewModel   = koinViewModel<HomeViewModel>()

    NavHost(
        navController    = navController,
        startDestination = Screen.MainPlayer.route
    ) {

        // ── Main Player (start destination) ─────────────────────────────────
        composable(Screen.MainPlayer.route) {
            MainPlayerScreen(
                viewModel            = playerViewModel,
                onNavigateToHome     = { navController.navigate(Screen.Home.route) },
                onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
                onNavigateToLyrics   = { songId ->
                    navController.navigate(Screen.Lyrics.createRoute(songId))
                }
            )
        }

        // ── Home Screen ──────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel            = homeViewModel,
                playerViewModel      = playerViewModel,
                onSongClick          = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // ── Lyrics Screen ────────────────────────────────────────────────────
        composable(
            route     = Screen.Lyrics.route,
            arguments = listOf(navArgument("songId") { type = NavType.LongType })
        ) { backStack ->
            val songId = backStack.arguments?.getLong("songId") ?: return@composable
            LyricsScreen(
                songId = songId,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Equalizer Screen ─────────────────────────────────────────────────
        composable(Screen.Equalizer.route) {
            val equalizerViewModel = koinViewModel<EqualizerViewModel>()
            EqualizerScreen(
                viewModel = equalizerViewModel,
                onBack    = { navController.popBackStack() }
            )
        }

        // ── Settings Screen ──────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack             = { navController.popBackStack() },
                onNavigateToAbout  = { navController.navigate(Screen.About.route) }
            )
        }

        // ── About Screen ─────────────────────────────────────────────────────
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
