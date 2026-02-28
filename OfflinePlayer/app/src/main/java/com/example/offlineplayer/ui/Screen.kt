package com.example.offlineplayer.ui

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Playlists : Screen("playlists", "Playlists")
    object Settings : Screen("settings", "Settings")
    object PlaylistDetails : Screen("playlist_details/{playlistId}", "Playlist") {
        fun createRoute(id: Int) = "playlist_details/$id"
    }
}