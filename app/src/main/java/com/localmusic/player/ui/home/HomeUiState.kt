package com.localmusic.player.ui.home

import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.domain.repository.RepeatMode
import com.localmusic.player.playlist.M3uPlaylist
import com.localmusic.player.ui.theme.AppThemeMode

/** Render state for the home library screen. */
data class HomeUiState(
    val songs: List<Song> = emptyList(),
    val artworkBySongId: Map<String, String> = emptyMap(),
    val selectedScreen: HomeScreenDestination = HomeScreenDestination.Home,
    val nowPlayingSong: Song? = null,
    val isPlaying: Boolean = false,
    val playbackProgress: Float = 0f,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val themeMode: AppThemeMode = AppThemeMode.FollowSystem,
    val isExternalArtworkDownloadEnabled: Boolean = true,
    val selectedFilter: LibraryFilter = LibraryFilter.AllSongs,
    val selectedBrowseValue: String? = null,
    val sortOrder: SortOrder = SortOrder.NewestAdded,
    val searchQuery: String = "",
    val importedPlaylists: List<M3uPlaylist> = emptyList(),
    val isCarMode: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshError: String? = null
)

enum class HomeScreenDestination(val label: String) {
    Home("Home"),
    NowPlaying("Now Playing"),
    Playlists("Playlists"),
    Favourites("Favourites"),
    Settings("Settings")
}
