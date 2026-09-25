package com.localmusic.player.ui.home

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localmusic.player.artwork.ArtworkDiskCache
import com.localmusic.player.artwork.ArtworkPreferences
import com.localmusic.player.artwork.EmbeddedArtworkExtractor
import com.localmusic.player.bluetooth.CarAudioDevice
import com.localmusic.player.bluetooth.CarModePreferences
import com.localmusic.player.cast.RemoteHandoffCoordinator
import com.localmusic.player.domain.model.LibraryBrowser
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.LibraryFacet
import com.localmusic.player.domain.model.LibraryQuery
import com.localmusic.player.domain.model.RadioStation
import com.localmusic.player.domain.model.RemoteStreamState
import com.localmusic.player.domain.model.RemoteStreamStatus
import com.localmusic.player.domain.model.SmartPlaylistRules
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.domain.repository.RepeatMode
import com.localmusic.player.domain.repository.RemoteStreamController
import com.localmusic.player.domain.usecase.AddFolderSourceUseCase
import com.localmusic.player.domain.usecase.ImportStreamSourceUseCase
import com.localmusic.player.domain.usecase.GetSongsByUrisUseCase
import com.localmusic.player.domain.usecase.ObserveLibraryPageUseCase
import com.localmusic.player.domain.usecase.PreviewRadioStationUseCase
import com.localmusic.player.domain.usecase.RefreshMusicLibraryUseCase
import com.localmusic.player.domain.usecase.RemoveFolderSourceUseCase
import com.localmusic.player.domain.usecase.SaveRadioStationUseCase
import com.localmusic.player.domain.usecase.SearchRadioStationsUseCase
import com.localmusic.player.domain.usecase.SetFavouriteUseCase
import com.localmusic.player.domain.usecase.StartPlaybackUseCase
import com.localmusic.player.domain.usecase.UpdateStreamMetadataUseCase
import com.localmusic.player.playlist.M3uPlaylist
import com.localmusic.player.playlist.M3uPlaylistCodec
import com.localmusic.player.playlist.M3uPlaylistEntry
import com.localmusic.player.playlist.PlaylistStore
import com.localmusic.player.playlist.toM3uEntry
import com.localmusic.player.playlist.withQueueFirst
import com.localmusic.player.playback.PlaybackPreferences
import com.localmusic.player.settings.LibraryPreferences
import com.localmusic.player.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ViewModel for library browsing, filtering, sorting, and search state. */
class HomeViewModel(
    observeLibraryPage: ObserveLibraryPageUseCase,
    private val getSongsByUris: GetSongsByUrisUseCase,
    private val refreshMusicLibrary: RefreshMusicLibraryUseCase,
    private val addFolderSource: AddFolderSourceUseCase,
    private val removeFolderSource: RemoveFolderSourceUseCase,
    private val folderSourceUris: () -> List<String> = { emptyList() },
    private val setFavourite: SetFavouriteUseCase,
    private val importStreamSource: ImportStreamSourceUseCase,
    private val searchRadioStations: SearchRadioStationsUseCase,
    private val saveRadioStation: SaveRadioStationUseCase,
    private val updateStreamMetadata: UpdateStreamMetadataUseCase,
    private val startPlayback: StartPlaybackUseCase,
    private val previewRadioStationUseCase: PreviewRadioStationUseCase,
    private val playlistStore: PlaylistStore? = null,
    private val artworkExtractor: EmbeddedArtworkExtractor? = null,
    private val artworkCache: ArtworkDiskCache? = null,
    private val artworkPreferences: ArtworkPreferences? = null,
    private val libraryPreferences: LibraryPreferences? = null,
    private val carModePreferences: CarModePreferences? = null,
    private val playbackPreferences: PlaybackPreferences? = null,
    private val remoteStreamController: RemoteStreamController? = null
) : ViewModel() {
    private val selectedFilter =
        MutableStateFlow(libraryPreferences?.defaultFilter() ?: LibraryFilter.AllSongs)
    private val selectedBrowseValue = MutableStateFlow<String?>(null)
    private val sortOrder =
        MutableStateFlow(libraryPreferences?.defaultSortOrder() ?: SortOrder.NewestAdded)
    private val searchQuery = MutableStateFlow("")
    private val loadedLibrarySongs = MutableStateFlow<List<Song>>(emptyList())
    private val radioSearchQuery = MutableStateFlow("")
    private val radioStations = MutableStateFlow<List<RadioStation>>(emptyList())
    private val isRadioSearchLoading = MutableStateFlow(false)
    private val radioSearchError = MutableStateFlow<String?>(null)
    private val hasSearchedRadioStations = MutableStateFlow(false)
    private val selectedScreen = MutableStateFlow(HomeScreenDestination.Home)
    private val nowPlayingSongId = MutableStateFlow<String?>(null)
    private val playbackQueueSongIds = MutableStateFlow<List<String>>(emptyList())
    private val playbackSongsById = MutableStateFlow<Map<String, Song>>(emptyMap())
    private val isPlaying = MutableStateFlow(false)
    private val playbackProgress = MutableStateFlow(0f)
    private val playbackDurationMillis = MutableStateFlow(0L)
    private val visualizerLevels = MutableStateFlow<List<Float>>(emptyList())
    private val isShuffleEnabled = MutableStateFlow(false)
    private val repeatMode = MutableStateFlow(RepeatMode.Off)
    private val remoteStreamState = MutableStateFlow(RemoteStreamState())
    private val themeMode = MutableStateFlow(AppThemeMode.FollowSystem)
    private val importedPlaylists = MutableStateFlow<List<M3uPlaylist>>(emptyList())
    private val playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    private val sourceFolderUris = MutableStateFlow(folderSourceUris())
    private val artworkBySongId = MutableStateFlow<Map<String, String>>(emptyMap())
    private val artworkCacheSizeBytes = MutableStateFlow(artworkCache?.sizeBytes() ?: 0L)
    private val isExternalArtworkDownloadEnabled =
        MutableStateFlow(artworkPreferences?.isExternalArtworkDownloadEnabled() ?: false)
    private val isVisualizerPreferred =
        MutableStateFlow(artworkPreferences?.isVisualizerPreferred() ?: false)
    private val isCarAudioDetected = MutableStateFlow(false)
    private val isCarModeManuallyEnabled =
        MutableStateFlow(carModePreferences?.isManuallyEnabled() ?: false)
    private val isKeepDisplayOnEnabled =
        MutableStateFlow(carModePreferences?.isKeepDisplayOnEnabled() ?: true)
    private val connectedCarAudioDevices = MutableStateFlow<List<CarAudioDevice>>(emptyList())
    private val markedCarDeviceIds =
        MutableStateFlow(carModePreferences?.markedCarDeviceIds().orEmpty())
    private val isRefreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow<String?>(null)
    private val playlistCodec = M3uPlaylistCodec()
    private val requestedArtworkSongIds = mutableSetOf<String>()
    private val playlistSaveRequests = Channel<M3uPlaylist>(Channel.UNLIMITED)
    private var hasRequestedInitialRefresh = false
    private var queueMutationGeneration = 0L
    private var hasObservedActiveQueue = false
    private var pendingPlaybackSongId: String? = null
    private val remoteHandoffCoordinator = RemoteHandoffCoordinator()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for (playlist in playlistSaveRequests) {
                playlistStore?.save(playlist)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val storedPlaylists = playlistStore?.playlists().orEmpty()
            withContext(Dispatchers.Main) {
                importedPlaylists.value = storedPlaylists
                ensureQueuePlaylist()
            }
            restorePersistedQueue(storedPlaylists)
        }
        viewModelScope.launch {
            importedPlaylists.collect { playlists ->
                val resolvedSongs = withContext(Dispatchers.IO) {
                    playlists.playableUris().chunked(PLAYLIST_URI_QUERY_LIMIT).flatMap { uris ->
                        getSongsByUris(uris)
                    }
                }
                playlistSongs.value = resolvedSongs
            }
        }
        viewModelScope.launch {
            startPlayback.observePlayback().collect { playback ->
                val pendingSongId = pendingPlaybackSongId
                if (!shouldApplyPlaybackSnapshot(
                        pendingSongId = pendingSongId,
                        observedSongId = playback.songId,
                        observedQueueSongIds = playback.queueSongIds
                    )
                ) return@collect
                if (pendingSongId != null) pendingPlaybackSongId = null
                if (playback.queueSongIds.isNotEmpty()) hasObservedActiveQueue = true
                nowPlayingSongId.value = playback.songId
                playbackQueueSongIds.value = playback.queueSongIds
                playbackSongsById.value[playback.songId]?.let { song ->
                    playbackPreferences?.setLastPlayedSongUri(song.uri)
                }
                updateRemoteStreamMetadata(playback)
                isPlaying.value = playback.isPlaying
                playbackProgress.value = playback.progress
                playbackDurationMillis.value = playback.durationMillis
                visualizerLevels.value = playback.visualizerLevels
                isShuffleEnabled.value = playback.isShuffleEnabled
                repeatMode.value = playback.repeatMode
                playback.errorMessage?.let { refreshError.value = "Playback failed: $it" }
            }
        }
        viewModelScope.launch {
            combine(nowPlayingSongId, playbackSongsById) { songId, songsById -> songsById[songId] }
                .collect { song -> remoteStreamController?.prepare(song) }
        }
        viewModelScope.launch {
            remoteStreamController?.state?.collect { remoteState ->
                remoteStreamState.value = remoteState
                val currentSong = playbackSongsById.value[nowPlayingSongId.value]
                if (remoteHandoffCoordinator.shouldPauseLocal(remoteState, currentSong)) {
                    runCatching { startPlayback.pause() }
                        .onSuccess { remoteHandoffCoordinator.markLocalPaused(remoteState) }
                        .onFailure { error ->
                            refreshError.value = error.message ?: "Could not pause local playback"
                        }
                }
            }
        }
    }

    private val selectionState =
        combine(selectedFilter, selectedBrowseValue, sortOrder, searchQuery, selectedScreen) { filter,
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
            .combine(sourceFolderUris) { state, folderUris ->
                state.copy(folderSourceUris = folderUris)
            }

    private val appearanceState =
        combine(themeMode, isExternalArtworkDownloadEnabled, isVisualizerPreferred) { mode,
                                                                                      externalArtworkEnabled,
                                                                                      visualizerPreferred ->
            AppearanceState(mode, externalArtworkEnabled, visualizerPreferred)
        }

    private val carModeState =
        combine(
            isCarAudioDetected,
            isCarModeManuallyEnabled,
            isKeepDisplayOnEnabled,
            connectedCarAudioDevices,
            markedCarDeviceIds
        ) { carAudioDetected, manuallyEnabled, keepDisplayOnEnabled, connectedDevices, markedDeviceIds ->
            CarModeState(
                isEnabled = carAudioDetected || manuallyEnabled,
                isCarAudioConnected = carAudioDetected,
                isManuallyEnabled = manuallyEnabled,
                isKeepDisplayOnEnabled = keepDisplayOnEnabled,
                connectedDevices = connectedDevices,
                markedDeviceIds = markedDeviceIds
            )
        }

    private val statusState =
        combine(importedPlaylists, carModeState, appearanceState, isRefreshing, refreshError) { playlists,
                                                                                                carMode,
                                                                                                appearance,
                                                                                                refreshing,
                                                                                                error ->
            HomeUiState(
                importedPlaylists = playlists,
                isCarMode = carMode.isEnabled,
                isCarAudioConnected = carMode.isCarAudioConnected,
                isCarModeManuallyEnabled = carMode.isManuallyEnabled,
                isKeepDisplayOnEnabled = carMode.isKeepDisplayOnEnabled,
                connectedCarAudioDevices = carMode.connectedDevices,
                markedCarDeviceIds = carMode.markedDeviceIds,
                themeMode = appearance.themeMode,
                isExternalArtworkDownloadEnabled =
                    appearance.isExternalArtworkDownloadEnabled,
                isVisualizerPreferred = appearance.isVisualizerPreferred,
                isRefreshing = refreshing,
                refreshError = error
            )
        }
            .combine(artworkCacheSizeBytes) { state, cacheSizeBytes ->
                state.copy(artworkCacheSizeBytes = cacheSizeBytes)
            }

    private val radioBrowserState =
        combine(
            radioSearchQuery, radioStations, isRadioSearchLoading, radioSearchError,
            hasSearchedRadioStations
        ) { query,
            stations,
            loading,
            error,
            hasSearched ->
            HomeUiState(
                radioSearchQuery = query,
                radioStations = stations,
                isRadioSearchLoading = loading,
                radioSearchError = error,
                hasSearchedRadioStations = hasSearched
            )
        }
            .combine(previewRadioStationUseCase.previewStation) { state, previewStation ->
                state.copy(previewRadioStation = previewStation)
            }
            .combine(previewRadioStationUseCase.previewError) { state, previewError ->
                state.copy(radioPreviewError = previewError)
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
            playbackQueueSongIds,
            isPlaying,
            playbackProgress,
            playbackDurationMillis
        ) { songId, queueSongIds, playing, progress, durationMillis ->
            PlaybackState(
                songId = songId,
                queueSongIds = queueSongIds,
                isPlaying = playing,
                progress = progress,
                durationMillis = durationMillis
            )
        }
            .combine(visualizerLevels) { playback, levels ->
                playback.copy(visualizerLevels = levels)
            }
            .combine(playbackModeState) { playback, playbackMode ->
                playback.copy(
                    isShuffleEnabled = playbackMode.isShuffleEnabled,
                    repeatMode = playbackMode.repeatMode
                )
            }

    private val controlsState =
        combine(selectionState, statusState, radioBrowserState) { selection, status, radio ->
            selection.copy(
                importedPlaylists = status.importedPlaylists,
                isCarMode = status.isCarMode,
                isCarModeManuallyEnabled = status.isCarModeManuallyEnabled,
                connectedCarAudioDevices = status.connectedCarAudioDevices,
                markedCarDeviceIds = status.markedCarDeviceIds,
                themeMode = status.themeMode,
                isExternalArtworkDownloadEnabled = status.isExternalArtworkDownloadEnabled,
                isVisualizerPreferred = status.isVisualizerPreferred,
                isRefreshing = status.isRefreshing,
                refreshError = status.refreshError,
                radioSearchQuery = radio.radioSearchQuery,
                radioStations = radio.radioStations,
                isRadioSearchLoading = radio.isRadioSearchLoading,
                radioSearchError = radio.radioSearchError,
                hasSearchedRadioStations = radio.hasSearchedRadioStations,
                previewRadioStation = radio.previewRadioStation,
                radioPreviewError = radio.radioPreviewError
            )
        }

    private val libraryQuery =
        combine(selectedFilter, selectedBrowseValue, sortOrder, searchQuery) { filter, browseValue, order, query ->
            LibraryQuery(
                filter = filter,
                browseValue = browseValue,
                sortOrder = order,
                searchQuery = query
            )
        }.combine(selectedScreen) { query, screen ->
            if (screen == HomeScreenDestination.Favourites) {
                query.copy(filter = LibraryFilter.Favourites, browseValue = null)
            } else {
                query
            }
        }

    val libraryPagingData: Flow<PagingData<Song>> =
        libraryQuery.flatMapLatest(observeLibraryPage::paged).cachedIn(viewModelScope)

    private val libraryFacets: Flow<List<LibraryFacet>> =
        libraryQuery.flatMapLatest { query ->
            if (query.filter.supportsFacets() && query.browseValue == null) {
                observeLibraryPage.facets(query)
            } else {
                flowOf(emptyList())
            }
        }

    private val libraryControlsState =
        combine(controlsState, libraryFacets) { state, facets ->
            state.copy(libraryFacets = facets)
        }

    val uiState: StateFlow<HomeUiState> =
        combine(
            loadedLibrarySongs,
            libraryControlsState,
            artworkBySongId,
            playbackState,
            playbackSongsById
        ) { loadedSongs, state, artwork, playback, activePlaybackSongs ->
            refreshArtwork(loadedSongs.take(ARTWORK_PREFETCH_LIMIT))
            val songsById = loadedSongs.associateBy(Song::id) + activePlaybackSongs
            val nowPlaying = songsById[playback.songId]
            state.copy(
                songs = loadedSongs,
                librarySongs = loadedSongs,
                favouriteSongs = if (state.selectedScreen == HomeScreenDestination.Favourites) loadedSongs else emptyList(),
                favouriteUris = loadedSongs.filter(Song::isFavourite).mapTo(mutableSetOf(), Song::uri),
                nowPlayingSong = nowPlaying,
                playbackQueue =
                    resolvePlaybackQueue(
                        queueSongIds = playback.queueSongIds,
                        songsById = songsById
                    ),
                isPlaying = nowPlaying != null && playback.isPlaying,
                playbackProgress = playback.progress,
                playbackDurationMillis = playback.durationMillis,
                visualizerLevels = playback.visualizerLevels,
                isShuffleEnabled = playback.isShuffleEnabled,
                repeatMode = playback.repeatMode,
                artworkBySongId = artwork
            )
        }
            .combine(playlistSongs) { state, resolvedPlaylistSongs ->
                refreshArtwork(resolvedPlaylistSongs)
                state.copy(
                    playlistArtworkByUri =
                        playlistArtworkByUri(resolvedPlaylistSongs, state.artworkBySongId)
                )
            }
            .combine(remoteStreamState) { state, remoteState ->
                val currentSong = state.nowPlayingSong
                val ownsPlayback =
                    currentSong != null &&
                            remoteState.requestedSongId == currentSong.id &&
                            remoteState.requestedUri == currentSong.uri &&
                            remoteState.status in
                            setOf(RemoteStreamStatus.Playing, RemoteStreamStatus.Paused)
                state.copy(
                    isPlaying = if (ownsPlayback) remoteState.status == RemoteStreamStatus.Playing else state.isPlaying,
                    remoteStreamState = remoteState
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
        loadedLibrarySongs.value = emptyList()
    }

    fun selectBrowseValue(value: String?) {
        selectedBrowseValue.value = value
        loadedLibrarySongs.value = emptyList()
    }

    fun selectSortOrder(order: SortOrder) {
        sortOrder.value = order
        loadedLibrarySongs.value = emptyList()
    }

    fun setDefaultFilter(filter: LibraryFilter) {
        libraryPreferences?.setDefaultFilter(filter)
        selectFilter(filter)
    }

    fun setDefaultSortOrder(order: SortOrder) {
        libraryPreferences?.setDefaultSortOrder(order)
        selectSortOrder(order)
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        loadedLibrarySongs.value = emptyList()
    }

    fun updateLoadedLibrarySongs(songs: List<Song>) {
        loadedLibrarySongs.value = songs
    }

    fun updateRadioSearchQuery(query: String) {
        radioSearchQuery.value = query
    }

    fun openRadioBrowser() {
        selectedScreen.value = HomeScreenDestination.RadioBrowser
    }

    fun searchRadioStations() {
        val query = radioSearchQuery.value.trim()
        if (query.isEmpty() || isRadioSearchLoading.value) return

        viewModelScope.launch {
            isRadioSearchLoading.value = true
            radioSearchError.value = null
            hasSearchedRadioStations.value = true
            runCatching { searchRadioStations(query) }
                .onSuccess { stations -> radioStations.value = stations }
                .onFailure { error ->
                    radioStations.value = emptyList()
                    radioSearchError.value = error.message ?: "Radio search failed"
                }
            isRadioSearchLoading.value = false
        }
    }

    fun selectScreen(destination: HomeScreenDestination) {
        selectedScreen.value = destination
        loadedLibrarySongs.value = emptyList()
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
            runCatching { addFolderSource.invoke(folderUri) }
                .onSuccess { sourceFolderUris.value = folderSourceUris() }
                .onFailure { error ->
                    refreshError.value = error.message ?: "Folder import failed"
                }
            isRefreshing.value = false
        }
    }

    fun removeFolderSource(folderUri: String) {
        viewModelScope.launch {
            isRefreshing.value = true
            refreshError.value = null
            runCatching { removeFolderSource.invoke(folderUri) }
                .onSuccess { sourceFolderUris.value = folderSourceUris() }
                .onFailure { error ->
                    refreshError.value = error.message ?: "Folder removal failed"
                }
            isRefreshing.value = false
        }
    }

    fun toggleFavourite(song: Song) {
        viewModelScope.launch {
            val isFavourite = !song.isFavourite
            runCatching { setFavourite(song.id, isFavourite) }
                .onSuccess {
                    playbackSongsById.value =
                        playbackSongsById.value.mapValues { (_, playbackSong) ->
                            if (playbackSong.id == song.id) playbackSong.copy(isFavourite = isFavourite)
                            else playbackSong
                        }
                }
                .onFailure { error ->
                    refreshError.value = error.message ?: "Favourite update failed"
                }
        }
    }

    fun togglePlaylistEntryFavourite(entry: M3uPlaylistEntry) {
        viewModelScope.launch {
            getSongsByUris(listOf(entry.uri)).firstOrNull()?.let(::toggleFavourite)
        }
    }

    fun playSong(song: Song) {
        val queue =
            importedPlaylists.value.firstOrNull { it.name == M3uPlaylist.QUEUE_NAME }
                ?: M3uPlaylist(name = M3uPlaylist.QUEUE_NAME, entries = emptyList())
        val isAlreadyQueued = queue.entries.any { it.uri == song.uri }
        val updatedQueue =
            if (isAlreadyQueued) queue
            else queue.copy(entries = queue.entries + song.toM3uEntry())

        queueMutationGeneration++
        refreshError.value = null
        if (!isAlreadyQueued) savePlaylist(updatedQueue)
        playbackPreferences?.setLastPlayedSongUri(song.uri)
        pendingPlaybackSongId = song.id
        nowPlayingSongId.value = song.id
        if (song.id !in playbackQueueSongIds.value) {
            playbackQueueSongIds.value = playbackQueueSongIds.value + song.id
        }
        playbackSongsById.value = playbackSongsById.value + (song.id to song)
        isPlaying.value = true
        playbackProgress.value = 0f
        playbackDurationMillis.value = 0L
        selectedScreen.value = HomeScreenDestination.NowPlaying

        viewModelScope.launch(Dispatchers.Default) {
            runCatching { startPlayback.enqueueAndPlay(song) }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    pendingPlaybackSongId = null
                    isPlaying.value = false
                    refreshError.value = error.message ?: "Playback failed"
                }
            }
        }
    }

    fun playFavourites(song: Song) {
        val favourites = uiState.value.favouriteSongs
        if (song !in favourites) return

        savePlaylist(
            M3uPlaylist(
                name = M3uPlaylist.QUEUE_NAME,
                entries = favourites.map(Song::toM3uEntry)
            )
        )
        playSongs(queue = favourites, startSong = song)
    }

    fun playQueuedSong(song: Song) {
        val queue = uiState.value.playbackQueue
        if (song !in queue) return

        playSongs(queue = queue, startSong = song)
    }

    fun playPlaylist(playlist: M3uPlaylist) {
        viewModelScope.launch {
            val songsByUri = getSongsByUris(playlist.entries.map(M3uPlaylistEntry::uri)).associateBy(Song::uri)
            val playlistSongs = playlist.entries.mapNotNull { entry -> songsByUri[entry.uri] }
            val firstSong = playlistSongs.firstOrNull()
            if (firstSong == null) {
                refreshError.value = "No playable songs found in ${playlist.name}"
                return@launch
            }
            savePlaylist(M3uPlaylist(name = M3uPlaylist.QUEUE_NAME, entries = playlist.entries))
            playSongs(queue = playlistSongs, startSong = firstSong)
        }
    }

    fun playPlaylistEntry(playlist: M3uPlaylist, entry: M3uPlaylistEntry) {
        viewModelScope.launch {
            val songsByUri = getSongsByUris(playlist.entries.map(M3uPlaylistEntry::uri)).associateBy(Song::uri)
            val playlistSongs = playlist.entries.mapNotNull { playlistEntry -> songsByUri[playlistEntry.uri] }
            val selectedSong = songsByUri[entry.uri]
            if (selectedSong == null || playlistSongs.isEmpty()) {
                refreshError.value = "No playable song found for ${entry.title}"
                return@launch
            }
            savePlaylist(M3uPlaylist(name = M3uPlaylist.QUEUE_NAME, entries = playlist.entries))
            playSongs(queue = playlistSongs, startSong = selectedSong)
        }
    }

    private fun playSongs(queue: List<Song>, startSong: Song) {
        val replacementQueue = resolveReplacementQueue(queue, startSong)
        queueMutationGeneration++
        refreshError.value = null
        savePlaylist(
            M3uPlaylist(
                name = M3uPlaylist.QUEUE_NAME,
                entries = replacementQueue.map(Song::toM3uEntry)
            )
        )
        playbackPreferences?.setLastPlayedSongUri(startSong.uri)
        pendingPlaybackSongId = startSong.id
        nowPlayingSongId.value = startSong.id
        playbackQueueSongIds.value = replacementQueue.map(Song::id)
        playbackSongsById.value = replacementQueue.associateBy(Song::id)
        isPlaying.value = true
        playbackProgress.value = 0f
        playbackDurationMillis.value = 0L
        selectedScreen.value = HomeScreenDestination.NowPlaying

        viewModelScope.launch(Dispatchers.Default) {
            runCatching { startPlayback(replacementQueue, startSong.id) }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    pendingPlaybackSongId = null
                    isPlaying.value = false
                    refreshError.value = error.message ?: "Playback failed"
                }
            }
        }
    }

    fun togglePlayback() {
        if (nowPlayingSongId.value == null) return
        val remoteState = remoteStreamState.value
        if (remoteStateMatchesCurrentSong(remoteState)) {
            runCatching {
                if (remoteState.status == RemoteStreamStatus.Playing) {
                    remoteStreamController?.pause()
                } else {
                    remoteStreamController?.play()
                }
            }.onFailure { error ->
                refreshError.value = error.message ?: "Cast playback control failed"
            }
            return
        }
        runCatching { if (isPlaying.value) startPlayback.pause() else startPlayback.resume() }
            .onSuccess { isPlaying.value = !isPlaying.value }
            .onFailure { error ->
                refreshError.value = error.message ?: "Playback control failed"
            }
    }

    fun skipToNext() {
        if (remoteStateMatchesCurrentSong(remoteStreamState.value)) return
        runCatching(startPlayback::skipToNext).onFailure { error ->
            refreshError.value = error.message ?: "Next track failed"
        }
    }

    fun skipToPrevious() {
        if (remoteStateMatchesCurrentSong(remoteStreamState.value)) return
        runCatching(startPlayback::skipToPrevious).onFailure { error ->
            refreshError.value = error.message ?: "Previous track failed"
        }
    }

    fun updatePlaybackProgress(progress: Float) {
        if (remoteStateMatchesCurrentSong(remoteStreamState.value)) return
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

    fun setVisualizerPreferred(preferred: Boolean) {
        artworkPreferences?.setVisualizerPreferred(preferred)
        isVisualizerPreferred.value = preferred
    }

    fun toggleShuffle() {
        if (remoteStateMatchesCurrentSong(remoteStreamState.value)) return
        runCatching { startPlayback.setShuffleEnabled(!isShuffleEnabled.value) }.onFailure { error
            ->
            refreshError.value = error.message ?: "Shuffle update failed"
        }
    }

    fun cycleRepeatMode() {
        if (remoteStateMatchesCurrentSong(remoteStreamState.value)) return
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

    fun clearArtworkCache() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    artworkCache?.clear()
                    artworkCache?.sizeBytes() ?: 0L
                }
            }
                .onSuccess { cacheSizeBytes ->
                    artworkBySongId.value = emptyMap()
                    requestedArtworkSongIds.clear()
                    artworkCacheSizeBytes.value = cacheSizeBytes
                }
                .onFailure { error ->
                    refreshError.value = error.message ?: "Artwork cache clear failed"
                }
        }
    }

    private fun refreshArtwork(songs: List<Song>) {
        val persistedArtwork =
            songs.mapNotNull { song -> song.artworkUri?.let { song.id to it } }
        if (persistedArtwork.isNotEmpty()) {
            artworkBySongId.value = artworkBySongId.value + persistedArtwork
        }

        val extractor = artworkExtractor ?: return
        val cachedArtworkIds = artworkBySongId.value.keys
        val persistedArtworkIds = persistedArtwork.mapTo(mutableSetOf()) { it.first }
        val missingSongs =
            songs.asSequence()
                .filter { song ->
                    song.id !in cachedArtworkIds && song.id !in persistedArtworkIds
                }
                .take(ARTWORK_PREFETCH_LIMIT)
                .filter { song -> requestedArtworkSongIds.add(song.id) }
                .toList()
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
            .onSuccess { playlist -> savePlaylist(playlist) }
            .onFailure { error ->
                refreshError.value = error.message ?: "Playlist import failed"
            }
    }

    override fun onCleared() {
        startPlayback.setVisualizerEnabled(false)
        remoteStreamController?.close()
        startPlayback.close()
        super.onCleared()
    }

    private fun remoteStateMatchesCurrentSong(state: RemoteStreamState): Boolean {
        if (state.status !in setOf(RemoteStreamStatus.Playing, RemoteStreamStatus.Paused)) return false
        val song = playbackSongsById.value[nowPlayingSongId.value] ?: return false
        return state.requestedSongId == song.id && state.requestedUri == song.uri
    }

    fun deletePlaylist(playlist: M3uPlaylist) {
        if (playlist.name == M3uPlaylist.QUEUE_NAME) return
        importedPlaylists.value = importedPlaylists.value.filterNot { it.name == playlist.name }
        viewModelScope.launch(Dispatchers.IO) { playlistStore?.delete(playlist.name) }
    }

    fun createPlaylist(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return

        val playlist = M3uPlaylist(name = normalizedName, entries = emptyList())
        savePlaylist(playlist)
    }

    fun renamePlaylist(playlist: M3uPlaylist, newName: String) {
        if (playlist.name == M3uPlaylist.QUEUE_NAME) return
        val normalizedName = newName.trim()
        if (normalizedName.isEmpty() || normalizedName == playlist.name) return

        val renamedPlaylist = playlist.copy(name = normalizedName)
        importedPlaylists.value =
            (importedPlaylists.value.filterNot {
                it.name == playlist.name || it.name == normalizedName
            } + renamedPlaylist)
                .withQueueFirst()
        viewModelScope.launch(Dispatchers.IO) {
            playlistStore?.delete(playlist.name)
            playlistStore?.save(renamedPlaylist)
        }
    }

    fun duplicatePlaylist(playlist: M3uPlaylist, name: String) {
        if (playlist.name == M3uPlaylist.QUEUE_NAME) return
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return

        val duplicate = playlist.copy(name = normalizedName)
        savePlaylist(duplicate)
    }

    fun removePlaylistEntry(playlist: M3uPlaylist, entryIndex: Int) {
        updatePlaylistEntries(playlist, entryIndex) { entries ->
            entries.filterIndexed { index, _ -> index != entryIndex }
        }
    }

    fun clearPlaylist(playlist: M3uPlaylist) {
        updatePlaylistEntries(playlist, 0) { emptyList() }
    }

    fun addStreamToPlaylist(playlist: M3uPlaylist, streamUrl: String) {
        if (playlist.name == M3uPlaylist.QUEUE_NAME) return
        viewModelScope.launch {
            runCatching { importStreamSource(streamUrl) }
                .onSuccess { stations ->
                    val currentPlaylist =
                        importedPlaylists.value.firstOrNull { it.name == playlist.name } ?: return@onSuccess
                    val newEntries =
                        stations.map(Song::toM3uEntry).filter { station ->
                            currentPlaylist.entries.none { it.uri == station.uri }
                        }
                    savePlaylist(currentPlaylist.copy(entries = currentPlaylist.entries + newEntries))
                }
                .onFailure { error ->
                    refreshError.value = error.message ?: "Stream import failed"
                }
        }
    }

    fun playRadioStation(station: RadioStation) {
        viewModelScope.launch {
            runCatching { saveRadioStation(station) }
                .onSuccess { song ->
                    val existingQueue =
                        importedPlaylists.value.firstOrNull {
                            it.name == M3uPlaylist.QUEUE_NAME
                        } ?: M3uPlaylist(name = M3uPlaylist.QUEUE_NAME, entries = emptyList())
                    val updatedQueue =
                        if (existingQueue.entries.any { it.uri == song.uri }) existingQueue
                        else existingQueue.copy(entries = existingQueue.entries + song.toM3uEntry())
                    savePlaylist(
                        updatedQueue
                    )
                    val songsByUri =
                        getSongsByUris(updatedQueue.entries.map(M3uPlaylistEntry::uri)).associateBy(Song::uri) +
                                (song.uri to song)
                    val queueSongs = updatedQueue.entries.mapNotNull { entry -> songsByUri[entry.uri] }
                    playSongs(queue = queueSongs, startSong = song)
                }
                .onFailure { error ->
                    refreshError.value = error.message ?: "Unable to save radio station"
                }
        }
    }

    fun previewRadioStation(station: RadioStation) {
        startPlayback.pause()
        previewRadioStationUseCase.play(station)
    }

    fun stopRadioPreview() {
        previewRadioStationUseCase.stop()
    }

    fun addRadioStationToPlaylist(station: RadioStation, playlistName: String) {
        viewModelScope.launch {
            runCatching { saveRadioStation(station) }
                .onSuccess { song -> addSongToPlaylistInternal(song, playlistName) }
                .onFailure { error ->
                    refreshError.value = error.message ?: "Unable to save radio station"
                }
        }
        previewRadioStationUseCase.close()
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
        queueMutationGeneration++
        viewModelScope.launch {
            val queue =
                importedPlaylists.value.firstOrNull { it.name == M3uPlaylist.QUEUE_NAME }
                    ?: M3uPlaylist(name = M3uPlaylist.QUEUE_NAME, entries = emptyList())
            if (queue.entries.any { it.uri == song.uri }) return@launch
            val updatedQueue = queue.copy(entries = queue.entries + song.toM3uEntry())
            savePlaylist(updatedQueue)
            playbackSongsById.value = playbackSongsById.value + (song.id to song)
            playbackQueueSongIds.value = playbackQueueSongIds.value + song.id
            startPlayback.enqueue(song)
        }
    }

    fun enqueuePlaylist(playlist: M3uPlaylist) {
        queueMutationGeneration++
        viewModelScope.launch {
            val queue =
                importedPlaylists.value.firstOrNull { it.name == M3uPlaylist.QUEUE_NAME } ?: return@launch
            val allUris = (queue.entries + playlist.entries).map(M3uPlaylistEntry::uri)
            val songsByUri = getSongsByUris(allUris).associateBy(Song::uri)
            val songsToEnqueue =
                resolvePlaylistEntriesToEnqueue(queue.entries, playlist.entries, songsByUri)
            val updatedQueue = queue.copy(entries = queue.entries + songsToEnqueue.map(Song::toM3uEntry))
            if (songsToEnqueue.isNotEmpty()) savePlaylist(updatedQueue)
            val queueSongs = updatedQueue.entries.mapNotNull { entry -> songsByUri[entry.uri] }
            if (queueSongs.isEmpty()) {
                refreshError.value = "No playable songs found in ${playlist.name}"
                return@launch
            }
            playbackSongsById.value = queueSongs.associateBy(Song::id)
            playbackQueueSongIds.value = queueSongs.map(Song::id)
            startPlayback.synchronizeQueue(queueSongs, nowPlayingSongId.value)
        }
    }

    fun clearQueue() {
        queueMutationGeneration++
        pendingPlaybackSongId = null
        playbackPreferences?.setLastPlayedSongUri(null)
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

    private suspend fun restorePersistedQueue(playlists: List<M3uPlaylist>) {
        val restorationGeneration = queueMutationGeneration
        val queue = playlists.firstOrNull { it.name == M3uPlaylist.QUEUE_NAME } ?: return
        val songsByUri = getSongsByUris(queue.entries.map(M3uPlaylistEntry::uri)).associateBy(Song::uri)
        val restoration =
            resolvePersistedQueue(
                queueEntries = queue.entries,
                songsByUri = songsByUri,
                lastPlayedSongUri = playbackPreferences?.lastPlayedSongUri()
            ) ?: return

        if (queueMutationGeneration != restorationGeneration || hasObservedActiveQueue) return

        withContext(Dispatchers.Main) {
            nowPlayingSongId.value = restoration.startSong.id
            playbackQueueSongIds.value = restoration.songs.map(Song::id)
            playbackSongsById.value = restoration.songs.associateBy(Song::id)
            isPlaying.value = false
            selectedScreen.value = HomeScreenDestination.NowPlaying
        }
        startPlayback.restoreQueue(restoration.songs, restoration.startSong.id)
    }

    private suspend fun synchronizeActiveQueue(
        queue: M3uPlaylist,
        additionalSongs: List<Song> = emptyList()
    ) {
        val songsByUri =
            getSongsByUris(queue.entries.map(M3uPlaylistEntry::uri)).associateBy(Song::uri) +
                    additionalSongs.associateBy(Song::uri)
        val queueSongs = queue.entries.mapNotNull { entry -> songsByUri[entry.uri] }
        playbackSongsById.value = queueSongs.associateBy(Song::id)
        playbackQueueSongIds.value = queueSongs.map(Song::id)
        startPlayback.synchronizeQueue(queueSongs, nowPlayingSongId.value)
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
            (importedPlaylists.value.filterNot { it.name == playlist.name } + playlist)
                .withQueueFirst()
        playlistSaveRequests.trySend(playlist)
    }

    private fun updateRemoteStreamMetadata(playback: com.localmusic.player.domain.repository.PlaybackSnapshot) {
        val songId = playback.songId ?: return
        val activeSong = playbackSongsById.value[songId] ?: return
        if (activeSong.source != com.localmusic.player.domain.model.SongSource.STREAM) return

        val updatedTitle = playback.title?.trim().takeUnless { it.isNullOrEmpty() } ?: activeSong.title
        val updatedArtist = playback.artist?.trim().takeUnless { it.isNullOrEmpty() } ?: activeSong.artist
        if (updatedTitle == activeSong.title && updatedArtist == activeSong.artist) return

        playbackSongsById.value =
            playbackSongsById.value +
                    (songId to activeSong.copy(title = updatedTitle, artist = updatedArtist))
        viewModelScope.launch {
            updateStreamMetadata(songId, updatedTitle, updatedArtist)
        }
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
        isCarAudioDetected.value = enabled
    }

    fun updateConnectedCarAudioDevices(devices: List<CarAudioDevice>) {
        connectedCarAudioDevices.value = devices
        refreshCarAudioDetection()
    }

    fun setCarDeviceMarked(deviceId: String, marked: Boolean) {
        carModePreferences?.setCarDeviceMarked(deviceId, marked)
        markedCarDeviceIds.value = carModePreferences?.markedCarDeviceIds().orEmpty()
        refreshCarAudioDetection()
    }

    private fun refreshCarAudioDetection() {
        isCarAudioDetected.value =
            connectedCarAudioDevices.value.any { device ->
                device.isLikelyCarDevice || device.id in markedCarDeviceIds.value
            }
    }

    fun setCarModeManuallyEnabled(enabled: Boolean) {
        carModePreferences?.setManuallyEnabled(enabled)
        isCarModeManuallyEnabled.value = enabled
    }

    fun setKeepDisplayOnEnabled(enabled: Boolean) {
        carModePreferences?.setKeepDisplayOnEnabled(enabled)
        isKeepDisplayOnEnabled.value = enabled
    }

    private fun LibraryFilter.supportsFacets(): Boolean =
        this == LibraryFilter.Artists ||
                this == LibraryFilter.Albums ||
                this == LibraryFilter.Genres ||
                this == LibraryFilter.Folders
}

internal fun projectLibrarySongs(
    songs: List<Song>,
    projection: LibraryProjection
): List<Song> =
    songs
        .filter { song ->
            song.matchesSearch(projection.searchQuery, projection.playlists) &&
                    song.matchesFilter(projection.filter) &&
                    LibraryBrowser.matches(
                        song,
                        projection.filter,
                        projection.browseValue
                    )
        }
        .sortedWith(projection.sortOrder.comparator())

private fun Song.matchesSearch(query: String, playlists: List<M3uPlaylist>): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return true

    val matchingPlaylistNames =
        playlists.filter { playlist -> playlist.entries.any { it.uri == uri } }.map { it.name }
    return listOf(
        title,
        artist,
        album,
        albumArtist,
        genre,
        composer,
        year?.toString().orEmpty(),
        comments,
        description,
        fileName,
        folderName
    )
        .plus(matchingPlaylistNames)
        .any { value ->
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

private data class PlaybackState(
    val songId: String?,
    val queueSongIds: List<String>,
    val isPlaying: Boolean,
    val progress: Float,
    val durationMillis: Long,
    val visualizerLevels: List<Float> = emptyList(),
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off
)

private data class PlaybackModeState(val isShuffleEnabled: Boolean, val repeatMode: RepeatMode)

private data class AppearanceState(
    val themeMode: AppThemeMode,
    val isExternalArtworkDownloadEnabled: Boolean,
    val isVisualizerPreferred: Boolean
)

private data class CarModeState(
    val isEnabled: Boolean,
    val isCarAudioConnected: Boolean,
    val isManuallyEnabled: Boolean,
    val isKeepDisplayOnEnabled: Boolean,
    val connectedDevices: List<CarAudioDevice>,
    val markedDeviceIds: Set<String>
)

internal data class LibraryProjection(
    val filter: LibraryFilter,
    val browseValue: String?,
    val sortOrder: SortOrder,
    val searchQuery: String,
    val playlists: List<M3uPlaylist>
)

internal fun resolvePlaybackQueue(
    queueSongIds: List<String>,
    songsById: Map<String, Song>
): List<Song> {
    return queueSongIds.distinct().mapNotNull(songsById::get)
}

internal fun resolveReplacementQueue(queue: List<Song>, startSong: Song): List<Song> {
    val uniqueQueue = queue.distinctBy(Song::id)
    return if (uniqueQueue.any { it.id == startSong.id }) uniqueQueue else uniqueQueue + startSong
}

internal fun shouldApplyPlaybackSnapshot(
    pendingSongId: String?,
    observedSongId: String?,
    observedQueueSongIds: List<String>
): Boolean =
    pendingSongId == null ||
            (observedSongId == pendingSongId && pendingSongId in observedQueueSongIds)

internal fun resolvePlaylistEntriesToEnqueue(
    queueEntries: List<M3uPlaylistEntry>,
    playlistEntries: List<M3uPlaylistEntry>,
    songsByUri: Map<String, Song>
): List<Song> {
    val queuedUris = queueEntries.mapTo(mutableSetOf()) { it.uri }
    return playlistEntries.mapNotNull { entry -> songsByUri[entry.uri] }.filter { song ->
        queuedUris.add(song.uri)
    }
}

internal data class PersistedQueueRestoration(
    val songs: List<Song>,
    val startSong: Song
)

internal fun resolvePersistedQueue(
    queueEntries: List<M3uPlaylistEntry>,
    songsByUri: Map<String, Song>,
    lastPlayedSongUri: String?
): PersistedQueueRestoration? {
    val songs = queueEntries.mapNotNull { entry -> songsByUri[entry.uri] }.distinctBy(Song::uri)
    val startSong = songs.firstOrNull { it.uri == lastPlayedSongUri } ?: songs.firstOrNull() ?: return null
    return PersistedQueueRestoration(songs = songs, startSong = startSong)
}

internal fun playlistArtworkByUri(
    songs: List<Song>,
    artworkBySongId: Map<String, String>
): Map<String, String> =
    songs.mapNotNull { song -> artworkBySongId[song.id]?.let { song.uri to it } }.toMap()

private fun List<M3uPlaylist>.playableUris(): List<String> =
    flatMap { playlist -> playlist.entries.map(M3uPlaylistEntry::uri) }.distinct()

private const val ARTWORK_PREFETCH_LIMIT = 64
private const val EXTERNAL_ARTWORK_PREFETCH_LIMIT = 4
private const val PLAYLIST_URI_QUERY_LIMIT = 500

/** Factory used until a dependency injection container is introduced. */
class HomeViewModelFactory(
    private val observeLibraryPage: ObserveLibraryPageUseCase,
    private val getSongsByUris: GetSongsByUrisUseCase,
    private val refreshMusicLibrary: RefreshMusicLibraryUseCase,
    private val addFolderSource: AddFolderSourceUseCase,
    private val removeFolderSource: RemoveFolderSourceUseCase,
    private val folderSourceUris: () -> List<String> = { emptyList() },
    private val setFavourite: SetFavouriteUseCase,
    private val importStreamSource: ImportStreamSourceUseCase,
    private val searchRadioStations: SearchRadioStationsUseCase,
    private val saveRadioStation: SaveRadioStationUseCase,
    private val updateStreamMetadata: UpdateStreamMetadataUseCase,
    private val startPlayback: StartPlaybackUseCase,
    private val previewRadioStation: PreviewRadioStationUseCase,
    private val playlistStore: PlaylistStore? = null,
    private val artworkExtractor: EmbeddedArtworkExtractor? = null,
    private val artworkCache: ArtworkDiskCache? = null,
    private val artworkPreferences: ArtworkPreferences? = null,
    private val libraryPreferences: LibraryPreferences? = null,
    private val carModePreferences: CarModePreferences? = null,
    private val playbackPreferences: PlaybackPreferences? = null,
    private val remoteStreamController: RemoteStreamController? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        return HomeViewModel(
            observeLibraryPage,
            getSongsByUris,
            refreshMusicLibrary,
            addFolderSource,
            removeFolderSource,
            folderSourceUris,
            setFavourite,
            importStreamSource,
            searchRadioStations,
            saveRadioStation,
            updateStreamMetadata,
            startPlayback,
            previewRadioStation,
            playlistStore,
            artworkExtractor,
            artworkCache,
            artworkPreferences,
            libraryPreferences,
            carModePreferences,
            playbackPreferences,
            remoteStreamController
        ) as
                T
    }
}
