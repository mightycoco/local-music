package com.localmusic.player.domain.model

/** Pure grouping and matching rules for the browsable local-library facets. */
object LibraryBrowser {
    fun valueCounts(songs: List<Song>, filter: LibraryFilter): List<Pair<String, Int>> =
            songs
                    .mapNotNull { song -> song.valueFor(filter) }
                    .groupingBy { it.lowercase() }
                    .fold(null as Pair<String, Int>?) { accumulator, value ->
                        (accumulator?.first ?: value) to ((accumulator?.second ?: 0) + 1)
                    }
                    .values
                    .filterNotNull()
                    .sortedBy { it.first.lowercase() }

    fun matches(song: Song, filter: LibraryFilter, selectedValue: String?): Boolean {
        if (selectedValue == null) return true
        return song.valueFor(filter)?.equals(selectedValue, ignoreCase = true) == true
    }

    private fun Song.valueFor(filter: LibraryFilter): String? =
            when (filter) {
                        LibraryFilter.Artists -> artist
                        LibraryFilter.Albums -> album
                        LibraryFilter.Genres -> genre
                        LibraryFilter.Folders -> folderName
                        else -> null
                    }
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
}
