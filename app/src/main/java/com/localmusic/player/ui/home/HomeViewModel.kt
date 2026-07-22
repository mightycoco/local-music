package com.localmusic.player.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localmusic.player.artwork.EmbeddedArtworkExtractor
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.domain.usecase.AddFolderSourceUseCase
import com.localmusic.player.domain.usecase.ObserveSongsUseCase
import com.localmusic.player.domain.usecase.RefreshMusicLibraryUseCase
import com.localmusic.player.domain.usecase.SetFavouriteUseCase
import com.localmusic.player.domain.usecase.StartPlaybackUseCase
import com.localmusic.player.playlist.M3uPlaylist
import com.localmusic.player.playlist.M3uPlaylistCodec
import com.localmusic.player.playlist.toM3uEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for library browsing, filtering, sorting, and search state. */
class HomeViewModel(
    observeSongs: ObserveSongsUseCase,
    private val refreshMusicLibrary: RefreshMusicLibraryUseCase,
    private val addFolderSource: AddFolderSourceUseCase,
    private val setFavourite: SetFavouriteUseCase,
    private val startPlayback: StartPlaybackUseCase,
    private val artworkExtractor: EmbeddedArtworkExtractor? = null
) : ViewModel() {
    private val selectedFilter = MutableStateFlow(LibraryFilter.AllSongs)
    private val sortOrder = MutableStateFlow(SortOrder.NewestAdded)
    private val searchQuery = MutableStateFlow("")
    private val importedPlaylists = MutableStateFlow<List<M3uPlaylist>>(emptyList())
    private val artworkBySongId = MutableStateFlow<Map<String, String>>(emptyMap())
    private val isCarMode = MutableStateFlow(false)
    private val isRefreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow<String?>(null)
    private val playlistCodec = M3uPlaylistCodec()

    private val selectionState = combine(
        selectedFilter,
        sortOrder,
        searchQuery
    ) { filter, order, query ->
        HomeUiState(
            selectedFilter = filter,
            sortOrder = order,
            searchQuery = query
        )
    }

    private val statusState = combine(
        importedPlaylists,
        isCarMode,
        isRefreshing,
        refreshError
    ) { playlists, carMode, refreshing, error ->
        HomeUiState(
            importedPlaylists = playlists,
            isCarMode = carMode,
            isRefreshing = refreshing,
            refreshError = error
        )
    }

    private val controlsState = combine(
        selectionState,
        statusState
    ) { selection, status ->
        selection.copy(
            importedPlaylists = status.importedPlaylists,
            isCarMode = status.isCarMode,
            isRefreshing = status.isRefreshing,
            refreshError = status.refreshError
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        observeSongs(),
        controlsState,
        artworkBySongId
    ) { songs, state, artwork ->
        refreshArtwork(songs)
        state.copy(
            songs = songs.applyLibraryProjection(state),
            artworkBySongId = artwork
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun selectFilter(filter: LibraryFilter) {
        selectedFilter.value = filter
    }

    fun selectSortOrder(order: SortOrder) {
        sortOrder.value = order
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun refreshLibrary() {
        if (isRefreshing.value) return

        viewModelScope.launch {
            isRefreshing.value = true
            refreshError.value = null
            runCatching { refreshMusicLibrary() }
                .onFailure { error -> refreshError.value = error.message ?: "Library refresh failed" }
            isRefreshing.value = false
        }
    }

    fun addFolderSource(folderUri: String) {
        viewModelScope.launch {
            isRefreshing.value = true
            refreshError.value = null
            runCatching { addFolderSource.invoke(folderUri) }
                .onFailure { error -> refreshError.value = error.message ?: "Folder import failed" }
            isRefreshing.value = false
        }
    }

    fun toggleFavourite(song: Song) {
        viewModelScope.launch {
            runCatching { setFavourite(song.id, !song.isFavourite) }
                .onFailure { error -> refreshError.value = error.message ?: "Favourite update failed" }
        }
    }

    fun playSong(song: Song) {
        runCatching { startPlayback(uiState.value.songs, song.id) }
            .onFailure { error -> refreshError.value = error.message ?: "Playback failed" }
    }

    private fun refreshArtwork(songs: List<Song>) {
        val extractor = artworkExtractor ?: return
        val missingSongs = songs.filter { it.id !in artworkBySongId.value }
        if (missingSongs.isEmpty()) return

        viewModelScope.launch {
            val resolvedArtwork = missingSongs.mapNotNull { song ->
                runCatching { extractor.artworkFor(song)?.toString() }
                    .getOrNull()
                    ?.let { song.id to it }
            }
            if (resolvedArtwork.isNotEmpty()) {
                artworkBySongId.value = artworkBySongId.value + resolvedArtwork
            }
        }
    }

    fun importPlaylist(name: String, content: String) {
        runCatching { playlistCodec.parse(name, content) }
            .onSuccess { playlist -> importedPlaylists.value = importedPlaylists.value + playlist }
            .onFailure { error -> refreshError.value = error.message ?: "Playlist import failed" }
    }

    fun exportCurrentPlaylist(): String = playlistCodec.export(
        M3uPlaylist(
            name = "Local Music Library",
            entries = uiState.value.songs.map { it.toM3uEntry() }
        )
    )

    fun updateCarMode(enabled: Boolean) {
        isCarMode.value = enabled
    }

    private fun List<Song>.applyLibraryProjection(state: HomeUiState): List<Song> = filter { song ->
        song.matchesSearch(state.searchQuery) && song.matchesFilter(state.selectedFilter)
    }.sortedWith(state.sortOrder.comparator())

    private fun Song.matchesSearch(query: String): Boolean {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return true

        return listOf(title, artist, album, fileName, folderName).any { value ->
            value.contains(normalizedQuery, ignoreCase = true)
        }
    }

    private fun Song.matchesFilter(filter: LibraryFilter): Boolean = when (filter) {
        LibraryFilter.AllSongs,
        LibraryFilter.Artists,
        LibraryFilter.Albums,
        LibraryFilter.Genres,
        LibraryFilter.Folders -> true
        LibraryFilter.RecentlyAdded -> dateAddedEpochSeconds > 0L
        LibraryFilter.RecentlyPlayed -> lastPlayedEpochMillis != null
        LibraryFilter.MostPlayed -> playCount > 0
        LibraryFilter.Favourites -> isFavourite
    }

    private fun SortOrder.comparator(): Comparator<Song> = when (this) {
        SortOrder.NewestAdded,
        SortOrder.DateAdded -> compareByDescending<Song> { it.dateAddedEpochSeconds }.thenBy { it.title.lowercase() }
        SortOrder.Name -> compareBy<Song> { it.title.lowercase() }.thenBy { it.artist.lowercase() }
        SortOrder.Artist -> compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
        SortOrder.Album -> compareBy<Song> { it.album.lowercase() }.thenBy { it.title.lowercase() }
        SortOrder.Duration -> compareByDescending<Song> { it.durationMillis }.thenBy { it.title.lowercase() }
        SortOrder.RecentlyPlayed -> compareByDescending<Song> { it.lastPlayedEpochMillis ?: 0L }.thenBy { it.title.lowercase() }
        SortOrder.MostPlayed -> compareByDescending<Song> { it.playCount }.thenBy { it.title.lowercase() }
    }
}

/** Factory used until a dependency injection container is introduced. */
class HomeViewModelFactory(
    private val observeSongs: ObserveSongsUseCase,
    private val refreshMusicLibrary: RefreshMusicLibraryUseCase,
    private val addFolderSource: AddFolderSourceUseCase,
    private val setFavourite: SetFavouriteUseCase,
    private val startPlayback: StartPlaybackUseCase,
    private val artworkExtractor: EmbeddedArtworkExtractor? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        return HomeViewModel(observeSongs, refreshMusicLibrary, addFolderSource, setFavourite, startPlayback, artworkExtractor) as T
    }
}
