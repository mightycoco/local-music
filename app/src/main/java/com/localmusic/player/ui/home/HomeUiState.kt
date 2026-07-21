package com.localmusic.player.ui.home

import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder

/** Render state for the home library screen. */
data class HomeUiState(
    val songs: List<Song> = emptyList(),
    val selectedFilter: LibraryFilter = LibraryFilter.AllSongs,
    val sortOrder: SortOrder = SortOrder.NewestAdded,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val refreshError: String? = null
)
