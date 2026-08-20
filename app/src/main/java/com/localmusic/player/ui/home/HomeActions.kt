package com.localmusic.player.ui.home

import androidx.compose.runtime.Immutable
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.RadioStation
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.playlist.M3uPlaylist
import com.localmusic.player.playlist.M3uPlaylistEntry
import com.localmusic.player.ui.theme.AppThemeMode

@Immutable
internal data class AppActions(
    val navigation: NavigationActions,
    val library: LibraryActions,
    val songs: SongActions,
    val playlists: PlaylistFeatureActions,
    val radio: RadioActions,
    val settings: SettingsFeatureActions
)

@Immutable
internal data class NavigationActions(
    val selectScreen: (HomeScreenDestination) -> Unit,
    val requestAudioPermission: () -> Unit
)

@Immutable
internal data class LibraryActions(
    val updateSearch: (String) -> Unit,
    val selectFilter: (LibraryFilter) -> Unit,
    val selectBrowseValue: (String?) -> Unit,
    val selectSortOrder: (SortOrder) -> Unit,
    val addFolderSource: () -> Unit
)

@Immutable
internal data class SongActions(
    val play: (Song) -> Unit,
    val playFavourites: (Song) -> Unit,
    val playQueued: (Song) -> Unit,
    val toggleFavourite: (Song) -> Unit,
    val addToPlaylist: (Song, String) -> Unit,
    val addToQueue: (Song) -> Unit
)

@Immutable
internal data class SongListActions(
    val select: (Song) -> Unit,
    val toggleFavourite: (Song) -> Unit,
    val createPlaylist: (String) -> Unit,
    val addToPlaylist: (Song, String) -> Unit,
    val addToQueue: (Song) -> Unit
)

internal fun SongActions.forList(
    select: (Song) -> Unit = play,
    createPlaylist: (String) -> Unit
): SongListActions =
    SongListActions(
        select = select,
        toggleFavourite = toggleFavourite,
        createPlaylist = createPlaylist,
        addToPlaylist = addToPlaylist,
        addToQueue = addToQueue
    )

@Immutable
internal data class PlaylistFeatureActions(
    val catalog: PlaylistCatalogActions,
    val documents: PlaylistDocumentActions,
    val editor: PlaylistEditorActions,
    val nowPlaying: NowPlayingPlaylistActions
)

@Immutable
internal data class PlaylistCatalogActions(
    val delete: (M3uPlaylist) -> Unit,
    val create: (String) -> Unit,
    val rename: (M3uPlaylist, String) -> Unit,
    val duplicate: (M3uPlaylist, String) -> Unit,
    val play: (M3uPlaylist) -> Unit,
    val enqueue: (M3uPlaylist) -> Unit,
    val clearQueue: () -> Unit
)

@Immutable
internal data class PlaylistDocumentActions(
    val import: () -> Unit,
    val exportLibrary: () -> Unit,
    val exportPlaylist: (M3uPlaylist) -> Unit
)

@Immutable
internal data class PlaylistEditorActions(
    val playEntry: (M3uPlaylist, M3uPlaylistEntry) -> Unit,
    val moveEntry: (M3uPlaylist, Int, Int) -> Unit,
    val toggleEntryFavourite: (M3uPlaylistEntry) -> Unit,
    val removeEntry: (M3uPlaylist, Int) -> Unit,
    val clear: (M3uPlaylist) -> Unit,
    val addStream: (M3uPlaylist, String) -> Unit,
    val openRadioBrowser: () -> Unit
)

@Immutable
internal data class NowPlayingPlaylistActions(
    val addToPlaylist: (String) -> Unit,
    val addToQueue: () -> Unit
)

@Immutable
internal data class RadioActions(
    val updateQuery: (String) -> Unit,
    val search: () -> Unit,
    val preview: (RadioStation) -> Unit,
    val stopPreview: () -> Unit,
    val addToPlaylist: (RadioStation, String) -> Unit
)

@Immutable
internal data class SettingsFeatureActions(
    val folders: FolderActions,
    val appearance: AppearanceActions,
    val preferences: PreferenceActions
)

@Immutable
internal data class FolderActions(
    val add: () -> Unit,
    val remove: (String) -> Unit
)

@Immutable
internal data class AppearanceActions(
    val selectTheme: (AppThemeMode) -> Unit,
    val clearArtworkCache: () -> Unit,
    val setExternalArtworkDownloadEnabled: (Boolean) -> Unit,
    val setVisualizerPreferred: (Boolean) -> Unit
)

@Immutable
internal data class PreferenceActions(
    val setDefaultFilter: (LibraryFilter) -> Unit,
    val setDefaultSortOrder: (SortOrder) -> Unit,
    val setCarModeManuallyEnabled: (Boolean) -> Unit,
    val setCarDeviceMarked: (String, Boolean) -> Unit
)