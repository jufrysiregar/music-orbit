package com.musicorbit.android.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.MutableStateFlow
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

/** Shared-element keys used by the mini player <-> full player album-art morph. */
object OrbitKeys {
    const val AlbumArt  = "orbit_album_art"
    const val Title     = "orbit_title"
    const val Artist    = "orbit_artist"
}

private const val NAV_DURATION = 320

@androidx.compose.animation.ExperimentalSharedTransitionApi
@Composable
fun AppNavGraph(
    deepLinkPlayer: MutableStateFlow<Boolean> = MutableStateFlow(false),
    onExitApp: () -> Unit = {}
) {
    val navController   = rememberNavController()
    val playerViewModel = koinViewModel<PlayerViewModel>()
    val homeViewModel   = koinViewModel<HomeViewModel>()

    val currentBackStack = navController.currentBackStackEntryAsState()
    val currentRoute     = currentBackStack.value?.destination?.route

    // When the widget's album art deep-links us, jump straight into the Main Player screen
    // (it renders the current / last displayed song from the shared PlayerViewModel).
    val openPlayer by deepLinkPlayer.collectAsState()
    LaunchedEffect(openPlayer) {
        if (openPlayer) {
            navController.navigate(Screen.MainPlayer.route) {
                launchSingleTop = true
                restoreState    = true
            }
            deepLinkPlayer.value = false
        }
    }

    BackHandler(enabled = currentRoute == Screen.Home.route) {
        onExitApp()
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController    = navController,
                startDestination = if (deepLinkPlayer.value) Screen.MainPlayer.route else Screen.Home.route,
                enterTransition  = {
                    slideInHorizontally(tween(NAV_DURATION)) { it / 4 } +
                        fadeIn(tween(NAV_DURATION))
                },
                exitTransition   = {
                    slideOutHorizontally(tween(NAV_DURATION)) { -it / 4 } +
                        fadeOut(tween(NAV_DURATION))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(NAV_DURATION)) { -it / 4 } +
                        fadeIn(tween(NAV_DURATION))
                },
                popExitTransition  = {
                    slideOutHorizontally(tween(NAV_DURATION)) { it / 4 } +
                        fadeOut(tween(NAV_DURATION))
                }
            ) {

                // ── Home (root / start destination) ─────────────────────────
                composable(Screen.Home.route) {
                    ProvideNavScope(this) {
                        HomeScreen(
                            viewModel            = homeViewModel,
                            playerViewModel      = playerViewModel,
                            onSongClick          = { _ ->
                                navController.navigate(Screen.MainPlayer.route) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            }
                        )
                    }
                }

                // ── Main Player ──────────────────────────────────────────────
                composable(Screen.MainPlayer.route) {
                    ProvideNavScope(this) {
                        MainPlayerScreen(
                            viewModel             = playerViewModel,
                            onNavigateToHome      = { navController.popBackStack() },
                            onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
                            onNavigateToLyrics    = { songId ->
                                navController.navigate(Screen.Lyrics.createRoute(songId))
                            }
                        )
                    }
                }

                // ── Lyrics ───────────────────────────────────────────────────
                composable(
                    route     = Screen.Lyrics.route,
                    arguments = listOf(navArgument("songId") { type = NavType.LongType })
                ) { backStack ->
                    val songId = backStack.arguments?.getLong("songId") ?: return@composable
                    ProvideNavScope(this) {
                        LyricsScreen(
                            songId = songId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                // ── Equalizer ────────────────────────────────────────────────
                composable(Screen.Equalizer.route) {
                    val equalizerViewModel = koinViewModel<EqualizerViewModel>()
                    ProvideNavScope(this) {
                        EqualizerScreen(
                            viewModel       = equalizerViewModel,
                            playerViewModel = playerViewModel,
                            onBack          = { navController.popBackStack() }
                        )
                    }
                }

                // ── Settings ─────────────────────────────────────────────────
                composable(Screen.Settings.route) {
                    ProvideNavScope(this) {
                        SettingsScreen(
                            onBack            = { navController.popBackStack() },
                            onNavigateToAbout = { navController.navigate(Screen.About.route) }
                        )
                    }
                }

                // ── About ────────────────────────────────────────────────────
                composable(Screen.About.route) {
                    ProvideNavScope(this) {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

/** Publishes the current route's [AnimatedVisibilityScope] so screens can use shared elements. */
@Composable
private fun ProvideNavScope(scope: AnimatedVisibilityScope, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimatedScope provides scope, content = content)
}
