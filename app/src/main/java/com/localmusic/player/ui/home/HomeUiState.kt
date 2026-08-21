package com.localmusic.player.ui.home

import com.localmusic.player.bluetooth.CarAudioDevice
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.LibraryFacet
import com.localmusic.player.domain.model.RadioStation
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.domain.repository.RepeatMode
import com.localmusic.player.playlist.M3uPlaylist
import com.localmusic.player.ui.theme.AppThemeMode

/** Render state for the home library screen. */
data class HomeUiState(
    val songs: List<Song> = emptyList(),
    val librarySongs: List<Song> = emptyList(),
    val favouriteSongs: List<Song> = emptyList(),
    val libraryFacets: List<LibraryFacet> = emptyList(),
    val artworkBySongId: Map<String, String> = emptyMap(),
    val selectedScreen: HomeScreenDestination = HomeScreenDestination.Home,
    val nowPlayingSong: Song? = null,
    val playbackQueue: List<Song> = emptyList(),
    val isPlaying: Boolean = false,
    val playbackProgress: Float = 0f,
    val playbackDurationMillis: Long = 0L,
    val visualizerLevels: List<Float> = emptyList(),
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val themeMode: AppThemeMode = AppThemeMode.FollowSystem,
    val isExternalArtworkDownloadEnabled: Boolean = true,
    val isVisualizerPreferred: Boolean = false,
    val selectedFilter: LibraryFilter = LibraryFilter.AllSongs,
    val selectedBrowseValue: String? = null,
    val sortOrder: SortOrder = SortOrder.NewestAdded,
    val searchQuery: String = "",
    val radioSearchQuery: String = "",
    val radioStations: List<RadioStation> = emptyList(),
    val isRadioSearchLoading: Boolean = false,
    val radioSearchError: String? = null,
    val hasSearchedRadioStations: Boolean = false,
    val previewRadioStation: RadioStation? = null,
    val radioPreviewError: String? = null,
    val folderSourceUris: List<String> = emptyList(),
    val artworkCacheSizeBytes: Long = 0L,
    val importedPlaylists: List<M3uPlaylist> = emptyList(),
    val isCarMode: Boolean = false,
    val isCarAudioConnected: Boolean = false,
    val isCarModeManuallyEnabled: Boolean = false,
    val isKeepDisplayOnEnabled: Boolean = true,
    val connectedCarAudioDevices: List<CarAudioDevice> = emptyList(),
    val markedCarDeviceIds: Set<String> = emptySet(),
    val isRefreshing: Boolean = false,
    val refreshError: String? = null
)

enum class HomeScreenDestination(val label: String) {
    Home("Home"),
    NowPlaying("Now Playing"),
    Playlists("Playlists"),
    Favourites("Favourites"),
    Settings("Settings"),
    PlaylistEditor("Playlist Editor"),
    RadioBrowser("Radio Browser")
}
