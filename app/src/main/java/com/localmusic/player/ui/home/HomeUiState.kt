package com.localmusic.player.ui.home

import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.playlist.M3uPlaylist

/** Render state for the home library screen. */
data class HomeUiState(
    val songs: List<Song> = emptyList(),
    val artworkBySongId: Map<String, String> = emptyMap(),
    val selectedFilter: LibraryFilter = LibraryFilter.AllSongs,
    val sortOrder: SortOrder = SortOrder.NewestAdded,
    val searchQuery: String = "",
    val importedPlaylists: List<M3uPlaylist> = emptyList(),
    val isCarMode: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshError: String? = null
)
