package com.musicorbit.android.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
fun AppNavGraph(onExitApp: () -> Unit = {}) {
    val navController   = rememberNavController()
    val playerViewModel = koinViewModel<PlayerViewModel>()
    val homeViewModel   = koinViewModel<HomeViewModel>()

    // Observe current route to know when we're at root
    val currentBackStack = navController.currentBackStackEntryAsState()
    val currentRoute     = currentBackStack.value?.destination?.route

    // When at root (Home), system back exits the app
    BackHandler(enabled = currentRoute == Screen.Home.route) {
        onExitApp()
    }

    NavHost(
        navController    = navController,
        startDestination = Screen.Home.route
    ) {

        // ── Home (root / start destination) ─────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel            = homeViewModel,
                playerViewModel      = playerViewModel,
                onSongClick          = { song ->
                    navController.navigate(Screen.MainPlayer.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // ── Main Player ──────────────────────────────────────────────────────
        composable(Screen.MainPlayer.route) {
            MainPlayerScreen(
                viewModel             = playerViewModel,
                onNavigateToHome      = {
                    navController.popBackStack()
                },
                onNavigateToEqualizer = {
                    navController.navigate(Screen.Equalizer.route)
                },
                onNavigateToLyrics    = { songId ->
                    navController.navigate(Screen.Lyrics.createRoute(songId))
                }
            )
        }

        // ── Lyrics ───────────────────────────────────────────────────────────
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

        // ── Equalizer ────────────────────────────────────────────────────────
        composable(Screen.Equalizer.route) {
            val equalizerViewModel = koinViewModel<EqualizerViewModel>()
            EqualizerScreen(
                viewModel = equalizerViewModel,
                onBack    = { navController.popBackStack() }
            )
        }

        // ── Settings ─────────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack            = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        // ── About ────────────────────────────────────────────────────────────
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
