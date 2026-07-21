package com.localmusic.player.domain.model

/** Sort orders supported by the library. */
enum class SortOrder(val label: String) {
    NewestAdded("Newest Added"),
    Name("Name"),
    Artist("Artist"),
    Album("Album"),
    Duration("Duration"),
    DateAdded("Date Added"),
    RecentlyPlayed("Recently Played"),
    MostPlayed("Most Played")
}
