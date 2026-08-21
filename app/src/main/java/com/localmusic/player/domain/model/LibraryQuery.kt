package com.localmusic.player.domain.model

/** A bounded, storage-backed library request for one visible result window. */
data class LibraryQuery(
    val filter: LibraryFilter = LibraryFilter.AllSongs,
    val browseValue: String? = null,
    val sortOrder: SortOrder = SortOrder.NewestAdded,
    val searchQuery: String = "",
    val limit: Int = DEFAULT_PAGE_SIZE,
    val offset: Int = 0
) {
    init {
        require(limit in 1..MAX_PAGE_SIZE) { "Library page size must be between 1 and $MAX_PAGE_SIZE." }
        require(offset >= 0) { "Library page offset cannot be negative." }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 100
        const val MAX_PAGE_SIZE = 500
    }
}