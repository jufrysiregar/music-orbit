package com.musicorbit.android.ui.navigation

sealed class Screen(val route: String) {
    object MainPlayer : Screen("main_player")
    object Home       : Screen("home")
    object Lyrics     : Screen("lyrics/{songId}") {
        fun createRoute(songId: Long) = "lyrics/$songId"
    }
    object Equalizer  : Screen("equalizer")
    object Settings   : Screen("settings")
    object About      : Screen("about")
}
