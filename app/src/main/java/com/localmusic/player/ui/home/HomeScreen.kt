package com.localmusic.player.ui.home

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.localmusic.player.bluetooth.CarModeDetector
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.playlist.M3uPlaylist
import com.localmusic.player.playlist.toM3uEntry
import com.localmusic.player.ui.theme.AppThemeMode
import kotlinx.coroutines.yield

internal const val VISUALIZER_FPS = 20

enum class Glyphs(val glyph: String) {
    HOME("⌂"),
    NOW_PLAYING("▷"),
    PLAYLISTS("⋮☰"),
    FAVOURITE("☆"),
    FAVOURITE_FULL("★"),
    SETTINGS("⫶"),
    PLAYINGINDICATOR("၊၊||၊|။||||။၊|။"),
    PLAYER_PREVIOUS("⏮"),
    PLAYER_NEXT("⏭"),
    PLAYER_PLAY("▶"),
    PLAYER_PAUSE("☐"),
    PLAYER_SHUFFLE("⇌"),
    PLAYER_NOSHUFFLE("⇉"),
    PLAYER_QUEUE("≡"),
    MORE("⋮"),
    NO_ARTWORK("╭∩╮( •̀_•́ )╭∩╮"),
    REORDER("≡")
}

@Composable
fun HomeRoute(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val audioPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
    val bluetoothPermission = Manifest.permission.BLUETOOTH_CONNECT
    var hasAudioPermission by remember {
        mutableStateOf(
                ContextCompat.checkSelfPermission(context, audioPermission) ==
                        PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                hasAudioPermission = granted
                if (granted) viewModel.refreshLibraryOnce()
            }
    val bluetoothPermissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
            ) { granted -> if (granted) viewModel.updateCarMode(context.isConnectedToCarAudio()) }
    val folderLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
            ) { folderUri ->
                folderUri ?: return@rememberLauncherForActivityResult
                context.contentResolver.takePersistableUriPermission(
                        folderUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.addFolderSource(folderUri.toString())
            }
    val playlistImportLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                    playlistUri ->
                playlistUri ?: return@rememberLauncherForActivityResult
                val name =
                        context.contentResolver.query(playlistUri, null, null, null, null)?.use {
                                cursor ->
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && index >= 0) cursor.getString(index)
                            else null
                        }
                                ?: "Imported Playlist"
                val content =
                        context.contentResolver
                                .openInputStream(playlistUri)
                                ?.bufferedReader()
                                ?.use { it.readText() }
                                .orEmpty()
                viewModel.importPlaylist(name, content)
            }
    var playlistToExport by remember { mutableStateOf<M3uPlaylist?>(null) }
    val playlistExportLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl")
            ) { playlistUri ->
                val playlist = playlistToExport
                if (playlistUri != null && playlist != null) {
                    context.contentResolver.openOutputStream(playlistUri)?.bufferedWriter()?.use {
                            writer ->
                        writer.write(viewModel.exportPlaylist(playlist))
                    }
                }
                playlistToExport = null
            }

    LaunchedEffect(hasAudioPermission) {
        yield()
        if (hasAudioPermission) viewModel.refreshLibraryOnce()
    }

    LaunchedEffect(Unit) {
        yield()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasBluetoothPermission =
                    ContextCompat.checkSelfPermission(context, bluetoothPermission) ==
                            PackageManager.PERMISSION_GRANTED
            if (hasBluetoothPermission) {
                viewModel.updateCarMode(context.isConnectedToCarAudio())
            } else {
                bluetoothPermissionLauncher.launch(bluetoothPermission)
            }
        } else {
            viewModel.updateCarMode(context.isConnectedToCarAudio())
        }
    }

    HomeScreen(
            uiState = uiState,
            hasAudioPermission = hasAudioPermission,
            onSearchChange = viewModel::updateSearchQuery,
            onScreenSelected = viewModel::selectScreen,
            onFilterSelected = viewModel::selectFilter,
            onBrowseValueSelected = viewModel::selectBrowseValue,
            onSortSelected = viewModel::selectSortOrder,
            onAddFolderSource = { folderLauncher.launch(null) },
            onRemoveFolderSource = { folderUri ->
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                            android.net.Uri.parse(folderUri),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                viewModel.removeFolderSource(folderUri)
            },
            onImportPlaylist = {
                playlistImportLauncher.launch(
                        arrayOf(
                                "audio/x-mpegurl",
                                "audio/mpegurl",
                                "text/plain",
                                "application/octet-stream"
                        )
                )
            },
            onExportPlaylist = {
                playlistToExport =
                        M3uPlaylist(
                                name = "Local Music Library",
                                entries = uiState.songs.map { it.toM3uEntry() }
                        )
                playlistExportLauncher.launch("local-music-library.m3u")
            },
            onExportIndividualPlaylist = { playlist ->
                playlistToExport = playlist
                playlistExportLauncher.launch("${playlist.name}.m3u")
            },
            onSongSelected = viewModel::playSong,
            onFavouriteToggle = viewModel::toggleFavourite,
            onDeletePlaylist = viewModel::deletePlaylist,
            onCreatePlaylist = viewModel::createPlaylist,
            onRenamePlaylist = viewModel::renamePlaylist,
            onDuplicatePlaylist = viewModel::duplicatePlaylist,
            onPlayPlaylist = viewModel::playPlaylist,
            onMovePlaylistEntry = viewModel::movePlaylistEntry,
            onAddNowPlayingToPlaylist = viewModel::addNowPlayingToPlaylist,
            onAddNowPlayingToQueue = viewModel::addNowPlayingToQueue,
            onClearQueue = viewModel::clearQueue,
            onClearPlaylist = viewModel::clearPlaylist,
            onAddSongToPlaylist = viewModel::addSongToPlaylist,
            onAddSongToQueue = viewModel::addSongToQueue,
            onPlayPause = viewModel::togglePlayback,
            onNext = viewModel::skipToNext,
            onPrevious = viewModel::skipToPrevious,
            onProgressChange = viewModel::updatePlaybackProgress,
            onShuffleToggle = viewModel::toggleShuffle,
            onRepeatCycle = viewModel::cycleRepeatMode,
            onVisualizerEnabledChange = viewModel::setVisualizerEnabled,
            onThemeSelected = viewModel::selectThemeMode,
            onClearArtworkCache = viewModel::clearArtworkCache,
            onDefaultFilterSelected = viewModel::setDefaultFilter,
            onDefaultSortOrderSelected = viewModel::setDefaultSortOrder,
            onExternalArtworkDownloadEnabledChange = viewModel::setExternalArtworkDownloadEnabled,
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    permissionLauncher.launch(audioPermission)
                }
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        uiState: HomeUiState,
        hasAudioPermission: Boolean,
        onSearchChange: (String) -> Unit,
        onScreenSelected: (HomeScreenDestination) -> Unit,
        onFilterSelected: (LibraryFilter) -> Unit,
        onBrowseValueSelected: (String?) -> Unit,
        onSortSelected: (SortOrder) -> Unit,
        onAddFolderSource: () -> Unit,
        onRemoveFolderSource: (String) -> Unit,
        onImportPlaylist: () -> Unit,
        onExportPlaylist: () -> Unit,
        onExportIndividualPlaylist: (M3uPlaylist) -> Unit,
        onSongSelected: (Song) -> Unit,
        onFavouriteToggle: (Song) -> Unit,
        onDeletePlaylist: (M3uPlaylist) -> Unit,
        onCreatePlaylist: (String) -> Unit,
        onRenamePlaylist: (M3uPlaylist, String) -> Unit,
        onDuplicatePlaylist: (M3uPlaylist, String) -> Unit,
        onPlayPlaylist: (M3uPlaylist) -> Unit,
        onMovePlaylistEntry: (M3uPlaylist, Int, Int) -> Unit,
        onAddNowPlayingToPlaylist: (String) -> Unit,
        onAddNowPlayingToQueue: () -> Unit,
        onClearQueue: () -> Unit,
        onClearPlaylist: (M3uPlaylist) -> Unit,
        onAddSongToPlaylist: (Song, String) -> Unit,
        onAddSongToQueue: (Song) -> Unit,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onProgressChange: (Float) -> Unit,
        onShuffleToggle: () -> Unit,
        onRepeatCycle: () -> Unit,
        onVisualizerEnabledChange: (Boolean) -> Unit,
        onThemeSelected: (AppThemeMode) -> Unit,
        onClearArtworkCache: () -> Unit,
        onDefaultFilterSelected: (LibraryFilter) -> Unit,
        onDefaultSortOrderSelected: (SortOrder) -> Unit,
        onExternalArtworkDownloadEnabledChange: (Boolean) -> Unit,
        onRequestPermission: () -> Unit
) {
    val libraryListState = rememberLazyListState()
    val swipeThreshold = 96.dp
    val showTopBar = uiState.selectedScreen != HomeScreenDestination.NowPlaying
    var playlistEditorName by remember { mutableStateOf<String?>(null) }
    var playlistEditorReturnDestination by remember {
        mutableStateOf(HomeScreenDestination.Playlists)
    }

    fun openPlaylistEditor(name: String, returnDestination: HomeScreenDestination) {
        playlistEditorName = name
        playlistEditorReturnDestination = returnDestination
        onScreenSelected(HomeScreenDestination.PlaylistEditor)
    }

    BackHandler(enabled = uiState.selectedScreen != HomeScreenDestination.Home) {
        onScreenSelected(
                if (uiState.selectedScreen == HomeScreenDestination.PlaylistEditor) {
                    playlistEditorReturnDestination
                } else {
                    HomeScreenDestination.Home
                }
        )
    }

    Scaffold(
            topBar = {
                if (showTopBar) {
                    TopAppBar(
                            title = {
                                val nowPlayingSong = uiState.nowPlayingSong
                                if (nowPlayingSong == null) {
                                    Text(
                                            text = "Local Music",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Light
                                    )
                                } else {
                                    MiniPlayer(
                                            song = nowPlayingSong,
                                            artworkUri = uiState.artworkBySongId[nowPlayingSong.id],
                                            isPlaying = uiState.isPlaying,
                                            progress = uiState.playbackProgress,
                                            onOpenNowPlaying = {
                                                onScreenSelected(HomeScreenDestination.NowPlaying)
                                            },
                                            onPrevious = onPrevious,
                                            onPlayPause = onPlayPause,
                                            onNext = onNext
                                    )
                                }
                            }
                    )
                }
            },
            bottomBar = {
                if (uiState.selectedScreen != HomeScreenDestination.PlaylistEditor) {
                    NavigationBar {
                        navigationDestinations.forEach { destination ->
                            NavigationBarItem(
                                    selected = uiState.selectedScreen == destination,
                                    onClick = { onScreenSelected(destination) },
                                    icon = {
                                        Text(text = destination.iconLabel(), fontSize = 28.sp)
                                    },
                                    label = null
                            )
                        }
                    }
                }
            }
    ) { padding ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 16.dp)
                                .pointerInput(uiState.selectedScreen) {
                                    var totalDrag = 0f
                                    var navigationTriggered = false
                                    detectHorizontalDragGestures(
                                            onDragStart = {
                                                totalDrag = 0f
                                                navigationTriggered = false
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                if (navigationTriggered) {
                                                    return@detectHorizontalDragGestures
                                                }

                                                totalDrag += dragAmount
                                                if (totalDrag >= swipeThreshold.toPx()) {
                                                    onScreenSelected(
                                                            uiState.selectedScreen.previous()
                                                    )
                                                    navigationTriggered = true
                                                } else if (totalDrag <= -swipeThreshold.toPx()) {
                                                    onScreenSelected(uiState.selectedScreen.next())
                                                    navigationTriggered = true
                                                }
                                            }
                                    )
                                }
        ) {
            if (!hasAudioPermission) {
                PermissionBanner(onRequestPermission = onRequestPermission)
                Spacer(modifier = Modifier.height(12.dp))
            }
            uiState.refreshError?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (uiState.isRefreshing) {
                Text(
                        text = "Refreshing local music...",
                        style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (uiState.isCarMode) {
                Text(
                        text = "Car Mode",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            AnimatedContent(
                    targetState = uiState.selectedScreen,
                    transitionSpec = {
                        val moveForward = initialState.movesForwardTo(targetState)
                        val direction = if (moveForward) 1 else -1
                        (slideInHorizontally(animationSpec = tween(280)) { it * direction } +
                                        fadeIn(animationSpec = tween(180)))
                                .togetherWith(
                                        slideOutHorizontally(animationSpec = tween(280)) {
                                            -it * direction
                                        } + fadeOut(animationSpec = tween(160))
                                )
                    },
                    label = "screenNavigation"
            ) { destination ->
                Column(modifier = Modifier.fillMaxSize()) {
                    when (destination) {
                        HomeScreenDestination.Home ->
                                LibraryContent(
                                        uiState = uiState,
                                        listState = libraryListState,
                                        onSearchChange = onSearchChange,
                                        onFilterSelected = onFilterSelected,
                                        onBrowseValueSelected = onBrowseValueSelected,
                                        onSortSelected = onSortSelected,
                                        onAddFolderSource = onAddFolderSource,
                                        onImportPlaylist = onImportPlaylist,
                                        onExportPlaylist = onExportPlaylist,
                                        onSongSelected = onSongSelected,
                                        onFavouriteToggle = onFavouriteToggle,
                                        onCreatePlaylist = onCreatePlaylist,
                                        onAddSongToPlaylist = onAddSongToPlaylist,
                                        onAddSongToQueue = onAddSongToQueue
                                )
                        HomeScreenDestination.NowPlaying ->
                                NowPlayingContent(
                                        uiState = uiState,
                                        onPlayPause = onPlayPause,
                                        onNext = onNext,
                                        onPrevious = onPrevious,
                                        onProgressChange = onProgressChange,
                                        onShuffleToggle = onShuffleToggle,
                                        onRepeatCycle = onRepeatCycle,
                                        onVisualizerEnabledChange = onVisualizerEnabledChange,
                                        onFavouriteToggle = onFavouriteToggle,
                                        onCreatePlaylist = onCreatePlaylist,
                                        onAddToPlaylist = onAddNowPlayingToPlaylist,
                                        onAddToQueue = onAddNowPlayingToQueue,
                                        onShowQueue = {
                                            openPlaylistEditor(
                                                    M3uPlaylist.QUEUE_NAME,
                                                    HomeScreenDestination.NowPlaying
                                            )
                                        },
                                        onReturnHome = {
                                            onScreenSelected(HomeScreenDestination.Home)
                                        }
                                )
                        HomeScreenDestination.Playlists ->
                                PlaylistContent(
                                        uiState = uiState,
                                        onDeletePlaylist = onDeletePlaylist,
                                        onRenamePlaylist = onRenamePlaylist,
                                        onDuplicatePlaylist = onDuplicatePlaylist,
                                        onPlayPlaylist = onPlayPlaylist,
                                        onExportPlaylist = onExportIndividualPlaylist,
                                        onClearQueue = onClearQueue,
                                        onOpenPlaylistEditor = { playlist ->
                                            openPlaylistEditor(
                                                    playlist.name,
                                                    HomeScreenDestination.Playlists
                                            )
                                        }
                                )
                        HomeScreenDestination.Favourites ->
                                SongList(
                                        songs = uiState.songs.filter { it.isFavourite },
                                        artworkBySongId = uiState.artworkBySongId,
                                        nowPlayingSongId = uiState.nowPlayingSong?.id,
                                        emptyTitle = "No favourites yet",
                                        emptyMessage =
                                                "Mark local songs as favourites to pin them here.",
                                        onSongSelected = onSongSelected,
                                        onFavouriteToggle = onFavouriteToggle,
                                        playlists = uiState.importedPlaylists,
                                        onCreatePlaylist = onCreatePlaylist,
                                        onAddSongToPlaylist = onAddSongToPlaylist,
                                        onAddSongToQueue = onAddSongToQueue
                                )
                        HomeScreenDestination.Settings ->
                                SettingsContent(
                                        uiState = uiState,
                                        onThemeSelected = onThemeSelected,
                                        onRemoveFolderSource = onRemoveFolderSource,
                                        onClearArtworkCache = onClearArtworkCache,
                                        onDefaultFilterSelected = onDefaultFilterSelected,
                                        onDefaultSortOrderSelected = onDefaultSortOrderSelected,
                                        onExternalArtworkDownloadEnabledChange =
                                                onExternalArtworkDownloadEnabledChange
                                )
                        HomeScreenDestination.PlaylistEditor -> {
                            val playlist =
                                    uiState.importedPlaylists.firstOrNull {
                                        it.name == playlistEditorName
                                    }
                            if (playlist == null) {
                                LaunchedEffect(playlistEditorName) {
                                    onScreenSelected(playlistEditorReturnDestination)
                                }
                            } else {
                                PlaylistEditorContent(
                                        playlist = playlist,
                                        onBack = {
                                            onScreenSelected(playlistEditorReturnDestination)
                                        },
                                        onMoveEntry = onMovePlaylistEntry,
                                        onClearPlaylist = onClearPlaylist,
                                        onDeletePlaylist = onDeletePlaylist
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun HomeScreenDestination.iconLabel(): String =
        when (this) {
            HomeScreenDestination.Home -> Glyphs.HOME.glyph
            HomeScreenDestination.NowPlaying -> Glyphs.NOW_PLAYING.glyph
            HomeScreenDestination.Playlists -> Glyphs.PLAYLISTS.glyph
            HomeScreenDestination.Favourites -> Glyphs.FAVOURITE.glyph
            HomeScreenDestination.Settings -> Glyphs.SETTINGS.glyph
            HomeScreenDestination.PlaylistEditor -> Glyphs.PLAYLISTS.glyph
        }

private fun HomeScreenDestination.previous(): HomeScreenDestination {
    val destinations = navigationDestinations
    return destinations[(ordinal - 1 + destinations.size) % destinations.size]
}

private fun HomeScreenDestination.next(): HomeScreenDestination {
    val destinations = navigationDestinations
    return destinations[(ordinal + 1) % destinations.size]
}

private fun HomeScreenDestination.movesForwardTo(target: HomeScreenDestination): Boolean =
        target == next() || (target != previous() && target.ordinal > ordinal)

private val navigationDestinations =
        HomeScreenDestination.entries.filterNot { it == HomeScreenDestination.PlaylistEditor }

@Composable
private fun PermissionBanner(onRequestPermission: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
                text = "Allow audio access to index local songs.",
                style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onRequestPermission) { Text("Allow Audio Access") }
    }
}

private fun android.content.Context.isConnectedToCarAudio(): Boolean {
    val bluetoothManager = getSystemService(BluetoothManager::class.java) ?: return false
    val adapter: BluetoothAdapter = bluetoothManager.adapter ?: return false
    val detector = CarModeDetector()
    return runCatching { adapter.bondedDevices.any(detector::isLikelyCarDevice) }
            .getOrDefault(false)
}
