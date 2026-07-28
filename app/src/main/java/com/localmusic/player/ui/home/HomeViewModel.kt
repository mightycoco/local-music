package com.localmusic.player.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localmusic.player.artwork.ArtworkPreferences
import com.localmusic.player.artwork.EmbeddedArtworkExtractor
import com.localmusic.player.domain.model.LibraryBrowser
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.SmartPlaylistRules
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.domain.repository.RepeatMode
import com.localmusic.player.domain.usecase.AddFolderSourceUseCase
import com.localmusic.player.domain.usecase.ObserveSongsUseCase
import com.localmusic.player.domain.usecase.RefreshMusicLibraryUseCase
import com.localmusic.player.domain.usecase.SetFavouriteUseCase
import com.localmusic.player.domain.usecase.StartPlaybackUseCase
import com.localmusic.player.playlist.M3uPlaylist
import com.localmusic.player.playlist.M3uPlaylistCodec
import com.localmusic.player.playlist.PlaylistStore
import com.localmusic.player.playlist.toM3uEntry
import com.localmusic.player.playlist.withQueueFirst
import com.localmusic.player.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ViewModel for library browsing, filtering, sorting, and search state. */
class HomeViewModel(
        observeSongs: ObserveSongsUseCase,
        private val refreshMusicLibrary: RefreshMusicLibraryUseCase,
        private val addFolderSource: AddFolderSourceUseCase,
        private val setFavourite: SetFavouriteUseCase,
        private val startPlayback: StartPlaybackUseCase,
        private val playlistStore: PlaylistStore? = null,
        private val artworkExtractor: EmbeddedArtworkExtractor? = null,
        private val artworkPreferences: ArtworkPreferences? = null
) : ViewModel() {
    private val selectedFilter = MutableStateFlow(LibraryFilter.AllSongs)
    private val selectedBrowseValue = MutableStateFlow<String?>(null)
    private val sortOrder = MutableStateFlow(SortOrder.NewestAdded)
    private val searchQuery = MutableStateFlow("")
    private val selectedScreen = MutableStateFlow(HomeScreenDestination.Home)
    private val nowPlayingSongId = MutableStateFlow<String?>(null)
    private val isPlaying = MutableStateFlow(false)
    private val playbackProgress = MutableStateFlow(0f)
    private val playbackDurationMillis = MutableStateFlow(0L)
    private val visualizerLevels = MutableStateFlow<List<Float>>(emptyList())
    private val isShuffleEnabled = MutableStateFlow(false)
    private val repeatMode = MutableStateFlow(RepeatMode.Off)
    private val themeMode = MutableStateFlow(AppThemeMode.FollowSystem)
    private val importedPlaylists = MutableStateFlow(playlistStore?.playlists().orEmpty())
    private val artworkBySongId = MutableStateFlow<Map<String, String>>(emptyMap())
    private val isExternalArtworkDownloadEnabled =
            MutableStateFlow(artworkPreferences?.isExternalArtworkDownloadEnabled() ?: true)
    private val isCarMode = MutableStateFlow(false)
    private val isRefreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow<String?>(null)
    private val playlistCodec = M3uPlaylistCodec()
    private val requestedArtworkSongIds = mutableSetOf<String>()
    private var hasRequestedInitialRefresh = false

    init {
        ensureQueuePlaylist()
        viewModelScope.launch {
            startPlayback.observePlayback().collect { playback ->
                nowPlayingSongId.value = playback.songId
                isPlaying.value = playback.isPlaying
                playbackProgress.value = playback.progress
                playbackDurationMillis.value = playback.durationMillis
                visualizerLevels.value = playback.visualizerLevels
                isShuffleEnabled.value = playback.isShuffleEnabled
                repeatMode.value = playback.repeatMode
            }
        }
    }

    private val selectionState =
            combine(selectedFilter, selectedBrowseValue, sortOrder, searchQuery, selectedScreen) {
                    filter,
                    browseValue,
                    order,
                    query,
                    screen ->
                HomeUiState(
                        selectedFilter = filter,
                        selectedBrowseValue = browseValue,
                        sortOrder = order,
                        searchQuery = query,
                        selectedScreen = screen
                )
            }

    private val appearanceState =
            combine(themeMode, isExternalArtworkDownloadEnabled) { mode, externalArtworkEnabled ->
                AppearanceState(mode, externalArtworkEnabled)
            }

    private val statusState =
            combine(importedPlaylists, isCarMode, appearanceState, isRefreshing, refreshError) {
                    playlists,
                    carMode,
                    appearance,
                    refreshing,
                    error ->
                HomeUiState(
                        importedPlaylists = playlists,
                        isCarMode = carMode,
                        themeMode = appearance.themeMode,
                        isExternalArtworkDownloadEnabled =
                                appearance.isExternalArtworkDownloadEnabled,
                        isRefreshing = refreshing,
                        refreshError = error
                )
            }

    private val playbackModeState =
            combine(isShuffleEnabled, repeatMode) { shuffleEnabled, selectedRepeatMode ->
                PlaybackModeState(
                        isShuffleEnabled = shuffleEnabled,
                        repeatMode = selectedRepeatMode
                )
            }

    private val playbackState =
            combine(
                            nowPlayingSongId,
                            isPlaying,
                            playbackProgress,
                            playbackDurationMillis,
                            visualizerLevels
                    ) { songId, playing, progress, durationMillis, levels ->
                PlaybackState(
                        songId = songId,
                        isPlaying = playing,
                        progress = progress,
                        durationMillis = durationMillis,
                        visualizerLevels = levels
                )
            }
                    .combine(playbackModeState) { playback, playbackMode ->
                        playback.copy(
                                isShuffleEnabled = playbackMode.isShuffleEnabled,
                                repeatMode = playbackMode.repeatMode
                        )
                    }

    private val controlsState =
            combine(selectionState, statusState) { selection, status ->
                selection.copy(
                        importedPlaylists = status.importedPlaylists,
                        isCarMode = status.isCarMode,
                        themeMode = status.themeMode,
                        isExternalArtworkDownloadEnabled = status.isExternalArtworkDownloadEnabled,
                        isRefreshing = status.isRefreshing,
                        refreshError = status.refreshError
                )
            }

    private val projectedSongs =
            combine(observeSongs(), controlsState) { songs, state -> songs to state }.map {
                    (songs, state) ->
                ProjectedSongs(
                        allSongs = songs,
                        visibleSongs =
                                withContext(Dispatchers.Default) {
                                    songs.applyLibraryProjection(state)
                                }
                )
            }

    val uiState: StateFlow<HomeUiState> =
            combine(projectedSongs, controlsState, artworkBySongId, playbackState) {
                            projected,
                            state,
                            artwork,
                            playback ->
                        refreshArtwork(projected.visibleSongs.take(ARTWORK_PREFETCH_LIMIT))
                        val nowPlaying = projected.allSongs.firstOrNull { it.id == playback.songId }
                        state.copy(
                                songs = projected.visibleSongs,
                                nowPlayingSong = nowPlaying,
                                isPlaying = nowPlaying != null && playback.isPlaying,
                                playbackProgress = playback.progress,
                                playbackDurationMillis = playback.durationMillis,
                                visualizerLevels = playback.visualizerLevels,
                                isShuffleEnabled = playback.isShuffleEnabled,
                                repeatMode = playback.repeatMode,
                                artworkBySongId = artwork
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5_000),
                            initialValue = HomeUiState()
                    )

    fun selectFilter(filter: LibraryFilter) {
        selectedFilter.value = filter
        selectedBrowseValue.value = null
    }

    fun selectBrowseValue(value: String?) {
        selectedBrowseValue.value = value
    }

    fun selectSortOrder(order: SortOrder) {
        sortOrder.value = order
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectScreen(destination: HomeScreenDestination) {
        selectedScreen.value = destination
        if (destination != HomeScreenDestination.NowPlaying) {
            startPlayback.setVisualizerEnabled(false)
        }
    }

    fun refreshLibraryOnce() {
        if (hasRequestedInitialRefresh) return
        hasRequestedInitialRefresh = true
        refreshLibrary()
    }

    fun refreshLibrary() {
        if (isRefreshing.value) return

        viewModelScope.launch {
            isRefreshing.value = true
            refreshError.value = null
            runCatching { refreshMusicLibrary() }.onFailure { error ->
                refreshError.value = error.message ?: "Library refresh failed"
            }
            isRefreshing.value = false
        }
    }

    fun addFolderSource(folderUri: String) {
        viewModelScope.launch {
            isRefreshing.value = true
            refreshError.value = null
            runCatching { addFolderSource.invoke(folderUri) }.onFailure { error ->
                refreshError.value = error.message ?: "Folder import failed"
            }
            isRefreshing.value = false
        }
    }

    fun toggleFavourite(song: Song) {
        viewModelScope.launch {
            runCatching { setFavourite(song.id, !song.isFavourite) }.onFailure { error ->
                refreshError.value = error.message ?: "Favourite update failed"
            }
        }
    }

    fun playSong(song: Song) {
        playSongs(queue = uiState.value.songs, startSong = song)
    }

    fun playPlaylist(playlist: M3uPlaylist) {
        val songsByUri = uiState.value.songs.associateBy(Song::uri)
        val playlistSongs = playlist.entries.mapNotNull { entry -> songsByUri[entry.uri] }
        val firstSong = playlistSongs.firstOrNull()
        if (firstSong == null) {
            refreshError.value = "No available local songs found in ${playlist.name}"
            return
        }

        playSongs(queue = playlistSongs, startSong = firstSong)
    }

    private fun playSongs(queue: List<Song>, startSong: Song) {
        nowPlayingSongId.value = startSong.id
        isPlaying.value = true
        playbackProgress.value = 0f
        playbackDurationMillis.value = 0L
        selectedScreen.value = HomeScreenDestination.NowPlaying

        viewModelScope.launch(Dispatchers.Default) {
            runCatching { startPlayback(queue, startSong.id) }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    isPlaying.value = false
                    refreshError.value = error.message ?: "Playback failed"
                }
            }
        }
    }

    fun togglePlayback() {
        if (nowPlayingSongId.value == null) return
        runCatching { if (isPlaying.value) startPlayback.pause() else startPlayback.resume() }
                .onSuccess { isPlaying.value = !isPlaying.value }
                .onFailure { error ->
                    refreshError.value = error.message ?: "Playback control failed"
                }
    }

    fun skipToNext() {
        moveNowPlaying(offset = 1)
    }

    fun skipToPrevious() {
        moveNowPlaying(offset = -1)
    }

    fun updatePlaybackProgress(progress: Float) {
        val coercedProgress = progress.coerceIn(0f, 1f)
        runCatching { startPlayback.seekTo(coercedProgress) }
                .onSuccess { playbackProgress.value = coercedProgress }
                .onFailure { error -> refreshError.value = error.message ?: "Seek failed" }
    }

    fun setVisualizerEnabled(enabled: Boolean) {
        runCatching { startPlayback.setVisualizerEnabled(enabled) }.onFailure { error ->
            refreshError.value = error.message ?: "Visualizer update failed"
        }
    }

    fun toggleShuffle() {
        runCatching { startPlayback.setShuffleEnabled(!isShuffleEnabled.value) }.onFailure { error
            ->
            refreshError.value = error.message ?: "Shuffle update failed"
        }
    }

    fun cycleRepeatMode() {
        val nextMode =
                when (repeatMode.value) {
                    RepeatMode.Off -> RepeatMode.All
                    RepeatMode.All -> RepeatMode.One
                    RepeatMode.One -> RepeatMode.Off
                }
        runCatching { startPlayback.setRepeatMode(nextMode) }.onFailure { error ->
            refreshError.value = error.message ?: "Repeat update failed"
        }
    }

    fun selectThemeMode(mode: AppThemeMode) {
        themeMode.value = mode
    }

    fun setExternalArtworkDownloadEnabled(enabled: Boolean) {
        artworkPreferences?.setExternalArtworkDownloadEnabled(enabled)
        isExternalArtworkDownloadEnabled.value = enabled
    }

    private fun moveNowPlaying(offset: Int) {
        val songs = uiState.value.songs
        if (songs.isEmpty()) return
        val currentIndex =
                songs.indexOfFirst { it.id == nowPlayingSongId.value }.takeIf { it >= 0 } ?: 0
        val nextIndex = Math.floorMod(currentIndex + offset, songs.size)
        playSong(songs[nextIndex])
    }

    private fun refreshArtwork(songs: List<Song>) {
        val extractor = artworkExtractor ?: return
        val cachedArtworkIds = artworkBySongId.value.keys
        val missingSongs =
                songs.filter { song ->
                    song.id !in cachedArtworkIds && requestedArtworkSongIds.add(song.id)
                }
        if (missingSongs.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val localArtwork =
                    missingSongs.mapNotNull { song ->
                        runCatching { extractor.localArtworkFor(song)?.toString() }
                                .getOrNull()
                                ?.let { song.id to it }
                    }
            if (localArtwork.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    artworkBySongId.value = artworkBySongId.value + localArtwork
                }
            }

            if (!isExternalArtworkDownloadEnabled.value) return@launch

            val localArtworkIds = localArtwork.mapTo(mutableSetOf()) { it.first }
            missingSongs
                    .filter { it.id !in localArtworkIds }
                    .take(EXTERNAL_ARTWORK_PREFETCH_LIMIT)
                    .mapNotNull { song ->
                        runCatching { extractor.downloadArtworkFor(song)?.toString() }
                                .getOrNull()
                                ?.let { song.id to it }
                    }
                    .takeIf { it.isNotEmpty() }
                    ?.let { downloadedArtwork ->
                        withContext(Dispatchers.Main) {
                            artworkBySongId.value = artworkBySongId.value + downloadedArtwork
                        }
                    }
        }
    }

    fun importPlaylist(name: String, content: String) {
        runCatching { playlistCodec.parse(name, content) }
                .onSuccess { playlist ->
                    importedPlaylists.value =
                            playlistStore?.save(playlist) ?: importedPlaylists.value + playlist
                }
                .onFailure { error ->
                    refreshError.value = error.message ?: "Playlist import failed"
                }
    }

    fun deletePlaylist(playlist: M3uPlaylist) {
        if (playlist.name == M3uPlaylist.QUEUE_NAME) return
        importedPlaylists.value =
                playlistStore?.delete(playlist.name)
                        ?: importedPlaylists.value.filterNot { it.name == playlist.name }
    }

    fun createPlaylist(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return

        val playlist = M3uPlaylist(name = normalizedName, entries = emptyList())
        importedPlaylists.value =
                playlistStore?.save(playlist)
                        ?: (importedPlaylists.value.filterNot { it.name == normalizedName } +
                                playlist)
    }

    fun renamePlaylist(playlist: M3uPlaylist, newName: String) {
        if (playlist.name == M3uPlaylist.QUEUE_NAME) return
        val normalizedName = newName.trim()
        if (normalizedName.isEmpty() || normalizedName == playlist.name) return

        val renamedPlaylist = playlist.copy(name = normalizedName)
        playlistStore?.delete(playlist.name)
        importedPlaylists.value =
                playlistStore?.save(renamedPlaylist)
                        ?: (importedPlaylists.value.filterNot {
                                    it.name == playlist.name || it.name == normalizedName
                                } + renamedPlaylist)
                                .withQueueFirst()
    }

    fun duplicatePlaylist(playlist: M3uPlaylist, name: String) {
        if (playlist.name == M3uPlaylist.QUEUE_NAME) return
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return

        val duplicate = playlist.copy(name = normalizedName)
        importedPlaylists.value =
                playlistStore?.save(duplicate)
                        ?: (importedPlaylists.value.filterNot { it.name == normalizedName } +
                                        duplicate)
                                .withQueueFirst()
    }

    fun removePlaylistEntry(playlist: M3uPlaylist, entryIndex: Int) {
        updatePlaylistEntries(playlist, entryIndex) { entries ->
            entries.filterIndexed { index, _ -> index != entryIndex }
        }
    }

    fun movePlaylistEntry(playlist: M3uPlaylist, entryIndex: Int, offset: Int) {
        updatePlaylistEntries(playlist, entryIndex) { entries ->
            val targetIndex = entryIndex + offset
            if (targetIndex !in entries.indices) return@updatePlaylistEntries entries

            entries.toMutableList().also { mutableEntries ->
                val entry = mutableEntries.removeAt(entryIndex)
                mutableEntries.add(targetIndex, entry)
            }
        }
    }

    fun addNowPlayingToPlaylist(name: String) {
        val song = uiState.value.nowPlayingSong ?: return
        addSongToPlaylist(song, name)
    }

    fun addNowPlayingToQueue() {
        uiState.value.nowPlayingSong?.let(::addSongToQueue)
    }

    fun addSongToPlaylist(song: Song, name: String) {
        addSongToPlaylistInternal(song, name)
    }

    fun addSongToQueue(song: Song) {
        if (addSongToPlaylistInternal(song, M3uPlaylist.QUEUE_NAME)) {
            runCatching { startPlayback.enqueue(song) }.onFailure { error ->
                refreshError.value = error.message ?: "Queue update failed"
            }
        }
    }

    fun clearQueue() {
        savePlaylist(M3uPlaylist(name = M3uPlaylist.QUEUE_NAME, entries = emptyList()))
        runCatching { startPlayback.clearQueue() }.onFailure { error ->
            refreshError.value = error.message ?: "Queue clear failed"
        }
    }

    private fun ensureQueuePlaylist() {
        if (importedPlaylists.value.none { it.name == M3uPlaylist.QUEUE_NAME }) {
            savePlaylist(M3uPlaylist(name = M3uPlaylist.QUEUE_NAME, entries = emptyList()))
        }
    }

    private fun addSongToPlaylistInternal(song: Song, name: String): Boolean {
        val playlist = importedPlaylists.value.firstOrNull { it.name == name } ?: return false
        val entry = song.toM3uEntry()
        if (playlist.entries.any { it.uri == entry.uri }) return false
        val updatedPlaylist = playlist.copy(entries = playlist.entries + entry)
        savePlaylist(updatedPlaylist)
        return true
    }

    private fun updatePlaylistEntries(
            playlist: M3uPlaylist,
            entryIndex: Int,
            transform:
                    (List<com.localmusic.player.playlist.M3uPlaylistEntry>) -> List<
                                    com.localmusic.player.playlist.M3uPlaylistEntry>
    ) {
        if (playlist.name == M3uPlaylist.QUEUE_NAME || entryIndex !in playlist.entries.indices)
                return
        val currentPlaylist =
                importedPlaylists.value.firstOrNull { it.name == playlist.name } ?: return
        savePlaylist(currentPlaylist.copy(entries = transform(currentPlaylist.entries)))
    }

    private fun savePlaylist(playlist: M3uPlaylist) {
        importedPlaylists.value =
                playlistStore?.save(playlist)
                        ?: (importedPlaylists.value.filterNot { it.name == playlist.name } +
                                        playlist)
                                .withQueueFirst()
    }

    fun exportCurrentPlaylist(): String =
            playlistCodec.export(
                    M3uPlaylist(
                            name = "Local Music Library",
                            entries = uiState.value.songs.map { it.toM3uEntry() }
                    )
            )

    fun exportPlaylist(playlist: M3uPlaylist): String = playlistCodec.export(playlist)

    fun updateCarMode(enabled: Boolean) {
        isCarMode.value = enabled
    }

    private fun List<Song>.applyLibraryProjection(state: HomeUiState): List<Song> =
            filter { song ->
                        song.matchesSearch(state.searchQuery) &&
                                song.matchesFilter(state.selectedFilter) &&
                                LibraryBrowser.matches(
                                        song,
                                        state.selectedFilter,
                                        state.selectedBrowseValue
                                )
                    }
                    .sortedWith(state.sortOrder.comparator())

    private fun Song.matchesSearch(query: String): Boolean {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return true

        return listOf(title, artist, album, fileName, folderName).any { value ->
            value.contains(normalizedQuery, ignoreCase = true)
        }
    }

    private fun Song.matchesFilter(filter: LibraryFilter): Boolean =
            when (filter) {
                LibraryFilter.AllSongs,
                LibraryFilter.Artists,
                LibraryFilter.Albums,
                LibraryFilter.Genres,
                LibraryFilter.Folders -> true
                LibraryFilter.RecentlyAdded -> dateAddedEpochSeconds > 0L
                LibraryFilter.RecentlyPlayed -> lastPlayedEpochMillis != null
                LibraryFilter.MostPlayed -> playCount > 0
                LibraryFilter.NeverPlayed -> SmartPlaylistRules.isNeverPlayed(this)
                LibraryFilter.LastThirtyDays ->
                        SmartPlaylistRules.wasPlayedWithinLastThirtyDays(
                                song = this,
                                nowEpochMillis = System.currentTimeMillis()
                        )
                LibraryFilter.Favourites -> isFavourite
            }

    private fun SortOrder.comparator(): Comparator<Song> =
            when (this) {
                SortOrder.NewestAdded, SortOrder.DateAdded ->
                        compareByDescending<Song> { it.dateAddedEpochSeconds }.thenBy {
                            it.title.lowercase()
                        }
                SortOrder.Name ->
                        compareBy<Song> { it.title.lowercase() }.thenBy { it.artist.lowercase() }
                SortOrder.Artist ->
                        compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
                SortOrder.Album ->
                        compareBy<Song> { it.album.lowercase() }.thenBy { it.title.lowercase() }
                SortOrder.Duration ->
                        compareByDescending<Song> { it.durationMillis }.thenBy {
                            it.title.lowercase()
                        }
                SortOrder.RecentlyPlayed ->
                        compareByDescending<Song> { it.lastPlayedEpochMillis ?: 0L }.thenBy {
                            it.title.lowercase()
                        }
                SortOrder.MostPlayed ->
                        compareByDescending<Song> { it.playCount }.thenBy { it.title.lowercase() }
            }
}

private data class PlaybackState(
        val songId: String?,
        val isPlaying: Boolean,
        val progress: Float,
        val durationMillis: Long,
        val visualizerLevels: List<Float>,
        val isShuffleEnabled: Boolean = false,
        val repeatMode: RepeatMode = RepeatMode.Off
)

private data class PlaybackModeState(val isShuffleEnabled: Boolean, val repeatMode: RepeatMode)

private data class AppearanceState(
        val themeMode: AppThemeMode,
        val isExternalArtworkDownloadEnabled: Boolean
)

private data class ProjectedSongs(val allSongs: List<Song>, val visibleSongs: List<Song>)

private const val ARTWORK_PREFETCH_LIMIT = 64
private const val EXTERNAL_ARTWORK_PREFETCH_LIMIT = 4

/** Factory used until a dependency injection container is introduced. */
class HomeViewModelFactory(
        private val observeSongs: ObserveSongsUseCase,
        private val refreshMusicLibrary: RefreshMusicLibraryUseCase,
        private val addFolderSource: AddFolderSourceUseCase,
        private val setFavourite: SetFavouriteUseCase,
        private val startPlayback: StartPlaybackUseCase,
        private val playlistStore: PlaylistStore? = null,
        private val artworkExtractor: EmbeddedArtworkExtractor? = null,
        private val artworkPreferences: ArtworkPreferences? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        return HomeViewModel(
                observeSongs,
                refreshMusicLibrary,
                addFolderSource,
                setFavourite,
                startPlayback,
                playlistStore,
                artworkExtractor,
                artworkPreferences
        ) as
                T
    }
}
