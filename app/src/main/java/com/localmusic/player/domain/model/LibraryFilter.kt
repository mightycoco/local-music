package com.localmusic.player.domain.model

/** User-facing library filters that remain visible on the home screen. */
enum class LibraryFilter(val label: String) {
    AllSongs("All Songs"),
    Artists("Artists"),
    Albums("Albums"),
    Genres("Genres"),
    Folders("Folders"),
    RecentlyAdded("Recently Added"),
    RecentlyPlayed("Recently Played"),
    MostPlayed("Most Played")
}
