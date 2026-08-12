package com.localmusic.player.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.localmusic.player.bluetooth.CarAudioDevice
import com.localmusic.player.bluetooth.CarModeDetector
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.RadioStation
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.playlist.M3uPlaylist
import com.localmusic.player.playlist.M3uPlaylistCodec
import com.localmusic.player.playlist.M3uPlaylistEntry
import com.localmusic.player.playlist.toM3uEntry
import com.localmusic.player.ui.theme.AppThemeMode
import com.localmusic.player.ui.theme.UiAnimationTimings
import java.io.Reader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.abs

internal const val VISUALIZER_FPS = 20

internal object AppIcons {
    val Home = Icons.Outlined.Home
    val NowPlaying = Icons.Outlined.PlayCircle
    val Playlists = Icons.AutoMirrored.Outlined.PlaylistPlay
    val Favourite = Icons.Outlined.FavoriteBorder
    val Settings = Icons.Outlined.Settings
}

internal const val PLAYING_INDICATOR = "၊၊|၊|။|||။၊|။•"
internal const val NO_ARTWORK_GLYPH = "╭∩╮( •̀_•́ )╭∩╮"
internal const val NO_ARTWORK_THUMB_GLYPH = ".°•"

@Composable
fun HomeRoute(viewModel: HomeViewModel, uiState: HomeUiState) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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
    var hasBluetoothPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(context, bluetoothPermission) ==
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
        ) { granted ->
            hasBluetoothPermission = granted
            if (!granted) viewModel.updateConnectedCarAudioDevices(emptyList())
        }
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
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { playlistUri ->
            playlistUri ?: return@rememberLauncherForActivityResult
            val name =
                context.contentResolver.query(playlistUri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && index >= 0) cursor.getString(index)
                    else null
                }
                    ?: "Imported Playlist"
            coroutineScope.launch {
                val content =
                    withContext(Dispatchers.IO) {
                        context.contentResolver
                            .openInputStream(playlistUri)
                            ?.bufferedReader()
                            ?.use { it.readTextLimited(MAX_PLAYLIST_IMPORT_CHARS) }
                            .orEmpty()
                    }
                viewModel.importPlaylist(name, content)
            }
        }
    var playlistToExport by remember { mutableStateOf<M3uPlaylist?>(null) }
    val playlistExportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl")
        ) { playlistUri ->
            val playlist = playlistToExport
            if (playlistUri != null && playlist != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    context.contentResolver
                        .openOutputStream(playlistUri)
                        ?.bufferedWriter()
                        ?.use { writer -> M3uPlaylistCodec().write(playlist, writer) }
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
        if (!hasBluetoothPermission) {
            bluetoothPermissionLauncher.launch(bluetoothPermission)
        }
    }

    DisposableEffect(context, hasBluetoothPermission) {
        if (!hasBluetoothPermission) return@DisposableEffect onDispose {}

        val detector = CarModeDetector()
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val adapter = bluetoothManager?.adapter
        var a2dpProxy: BluetoothProfile? = null
        fun refreshConnectedDevices(proxy: BluetoothProfile) {
            val connectedDevices =
                runCatching {
                    proxy.connectedDevices.mapNotNull { device ->
                        device.safeAddress()?.let { deviceId ->
                            CarAudioDevice(
                                id = deviceId,
                                name = device.safeName(),
                                isLikelyCarDevice = detector.isLikelyCarDevice(device)
                            )
                        }
                    }
                }
                    .getOrDefault(emptyList())
            viewModel.updateConnectedCarAudioDevices(connectedDevices)
        }

        val profileListener =
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile != BluetoothProfile.A2DP) return

                    a2dpProxy = proxy
                    refreshConnectedDevices(proxy)
                }

                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.A2DP) {
                        a2dpProxy = null
                        viewModel.updateConnectedCarAudioDevices(emptyList())
                    }
                }
            }
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    receiverContext: android.content.Context,
                    intent: Intent
                ) {
                    if (intent.action != BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) return

                    a2dpProxy?.let(::refreshConnectedDevices)
                }
            }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        runCatching { adapter?.getProfileProxy(context, profileListener, BluetoothProfile.A2DP) }
        onDispose {
            context.unregisterReceiver(receiver)
            a2dpProxy?.let { proxy -> adapter?.closeProfileProxy(BluetoothProfile.A2DP, proxy) }
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
        onFavouriteSongSelected = viewModel::playFavourites,
        onQueueSongSelected = viewModel::playQueuedSong,
        onFavouriteToggle = viewModel::toggleFavourite,
        onDeletePlaylist = viewModel::deletePlaylist,
        onCreatePlaylist = viewModel::createPlaylist,
        onRenamePlaylist = viewModel::renamePlaylist,
        onDuplicatePlaylist = viewModel::duplicatePlaylist,
        onPlayPlaylist = viewModel::playPlaylist,
        onMovePlaylistEntry = viewModel::movePlaylistEntry,
        onPlaylistEntryFavouriteToggle = viewModel::togglePlaylistEntryFavourite,
        onRemovePlaylistEntry = viewModel::removePlaylistEntry,
        onAddNowPlayingToPlaylist = viewModel::addNowPlayingToPlaylist,
        onAddNowPlayingToQueue = viewModel::addNowPlayingToQueue,
        onClearQueue = viewModel::clearQueue,
        onClearPlaylist = viewModel::clearPlaylist,
        onAddStreamToPlaylist = viewModel::addStreamToPlaylist,
        onOpenRadioBrowser = viewModel::openRadioBrowser,
        onRadioSearchQueryChange = viewModel::updateRadioSearchQuery,
        onSearchRadioStations = viewModel::searchRadioStations,
        onPlayRadioStation = viewModel::playRadioStation,
        onAddRadioStationToPlaylist = viewModel::addRadioStationToPlaylist,
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
        onCarModeManuallyEnabledChange = viewModel::setCarModeManuallyEnabled,
        onCarDeviceMarkedChange = viewModel::setCarDeviceMarked,
        onExternalArtworkDownloadEnabledChange = viewModel::setExternalArtworkDownloadEnabled,
        onVisualizerPreferredChange = viewModel::setVisualizerPreferred,
        onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionLauncher.launch(audioPermission)
            }
        }
    )
}

private fun Reader.readTextLimited(maxCharacters: Int): String {
    val output = StringBuilder(minOf(maxCharacters, DEFAULT_PLAYLIST_BUFFER_CHARS))
    val buffer = CharArray(DEFAULT_PLAYLIST_BUFFER_CHARS)
    while (true) {
        val read = read(buffer)
        if (read < 0) return output.toString()
        require(output.length + read <= maxCharacters) { "Playlist file is too large" }
        output.append(buffer, 0, read)
    }
}

private const val MAX_PLAYLIST_IMPORT_CHARS = 4 * 1024 * 1024
private const val DEFAULT_PLAYLIST_BUFFER_CHARS = 8 * 1024

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
    onFavouriteSongSelected: (Song) -> Unit,
    onQueueSongSelected: (Song) -> Unit,
    onFavouriteToggle: (Song) -> Unit,
    onDeletePlaylist: (M3uPlaylist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (M3uPlaylist, String) -> Unit,
    onDuplicatePlaylist: (M3uPlaylist, String) -> Unit,
    onPlayPlaylist: (M3uPlaylist) -> Unit,
    onMovePlaylistEntry: (M3uPlaylist, Int, Int) -> Unit,
    onPlaylistEntryFavouriteToggle: (M3uPlaylistEntry) -> Unit,
    onRemovePlaylistEntry: (M3uPlaylist, Int) -> Unit,
    onAddNowPlayingToPlaylist: (String) -> Unit,
    onAddNowPlayingToQueue: () -> Unit,
    onClearQueue: () -> Unit,
    onClearPlaylist: (M3uPlaylist) -> Unit,
    onAddStreamToPlaylist: (M3uPlaylist, String) -> Unit,
    onOpenRadioBrowser: () -> Unit,
    onRadioSearchQueryChange: (String) -> Unit,
    onSearchRadioStations: () -> Unit,
    onPlayRadioStation: (RadioStation) -> Unit,
    onAddRadioStationToPlaylist: (RadioStation, String) -> Unit,
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
    onCarModeManuallyEnabledChange: (Boolean) -> Unit,
    onCarDeviceMarkedChange: (String, Boolean) -> Unit,
    onExternalArtworkDownloadEnabledChange: (Boolean) -> Unit,
    onVisualizerPreferredChange: (Boolean) -> Unit,
    onRequestPermission: () -> Unit
) {
    val libraryListState = rememberLazyListState()
    val showTopBar =
        uiState.selectedScreen != HomeScreenDestination.NowPlaying &&
                uiState.selectedScreen != HomeScreenDestination.RadioBrowser
    val screenSwipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
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
            if (uiState.selectedScreen == HomeScreenDestination.PlaylistEditor ||
                uiState.selectedScreen == HomeScreenDestination.RadioBrowser
            ) {
                playlistEditorReturnDestination
            } else {
                HomeScreenDestination.Home
            }
        )
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                val nowPlayingSong = uiState.nowPlayingSong
                if (nowPlayingSong == null) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Local Music",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Light
                            )
                        }
                    )
                } else {
                    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 2.dp) {
                        MiniPlayer(
                            song = nowPlayingSong,
                            artworkUri = uiState.artworkBySongId[nowPlayingSong.id],
                            isPlaying = uiState.isPlaying,
                            progress = uiState.playbackProgress,
                            isCarMode = uiState.isCarMode,
                            onOpenNowPlaying = {
                                onScreenSelected(HomeScreenDestination.NowPlaying)
                            },
                            onPrevious = onPrevious,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            onProgressChange = onProgressChange
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (uiState.selectedScreen != HomeScreenDestination.PlaylistEditor &&
                uiState.selectedScreen != HomeScreenDestination.RadioBrowser
            ) {
                NavigationBar {
                    navigationDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = uiState.selectedScreen == destination,
                            onClick = { onScreenSelected(destination) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon(),
                                    contentDescription = destination.name
                                )
                            },
                            label = null
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
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
            AnimatedContent(
                modifier =
                    Modifier.fillMaxSize().pointerInput(
                        uiState.selectedScreen,
                        screenSwipeThreshold
                    ) {
                        if (uiState.selectedScreen == HomeScreenDestination.PlaylistEditor ||
                            uiState.selectedScreen == HomeScreenDestination.RadioBrowser
                        ) {
                            return@pointerInput
                        }
                        awaitEachGesture {
                            val down =
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )
                            var drag = Offset.Zero
                            var consumedByChild = false
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                                val change =
                                    event.changes.firstOrNull { it.id == down.id }
                                        ?: break
                                consumedByChild = consumedByChild || change.isConsumed
                                drag += change.position - change.previousPosition
                            } while (change.pressed)

                            if (!consumedByChild &&
                                abs(drag.x) >= screenSwipeThreshold &&
                                abs(drag.x) > abs(drag.y) * 1.5f
                            ) {
                                onScreenSelected(
                                    if (drag.x > 0f) {
                                        uiState.selectedScreen.previous()
                                    } else {
                                        uiState.selectedScreen.next()
                                    }
                                )
                            }
                        }
                    },
                targetState = uiState.selectedScreen,
                transitionSpec = {
                    val moveForward = initialState.movesForwardTo(targetState)
                    val direction = if (moveForward) 1 else -1
                    (slideInHorizontally(
                        animationSpec =
                            tween(
                                UiAnimationTimings
                                    .SCREEN_NAVIGATION_SLIDE_MILLIS
                            )
                    ) { it * direction } +
                            fadeIn(
                                animationSpec =
                                    tween(
                                        UiAnimationTimings
                                            .SCREEN_NAVIGATION_FADE_IN_MILLIS
                                    )
                            ))
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec =
                                    tween(
                                        UiAnimationTimings
                                            .SCREEN_NAVIGATION_SLIDE_MILLIS
                                    )
                            ) { -it * direction } +
                                    fadeOut(
                                        animationSpec =
                                            tween(
                                                UiAnimationTimings
                                                    .SCREEN_NAVIGATION_FADE_OUT_MILLIS
                                            )
                                    )
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
                                onQueueSongSelected = onQueueSongSelected,
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
                                onCreatePlaylist = onCreatePlaylist,
                                onRenamePlaylist = onRenamePlaylist,
                                onDuplicatePlaylist = onDuplicatePlaylist,
                                onPlayPlaylist = onPlayPlaylist,
                                onImportPlaylist = onImportPlaylist,
                                onExportLibrary = onExportPlaylist,
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
                                songs = uiState.favouriteSongs,
                                artworkBySongId = uiState.artworkBySongId,
                                nowPlayingSongId = uiState.nowPlayingSong?.id,
                                isPlaying = uiState.isPlaying,
                                emptyTitle = "No favourites yet",
                                emptyMessage =
                                    "Mark songs or online streams as favourites to pin them here.",
                                onSongSelected = onFavouriteSongSelected,
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
                                onAddFolderSource = onAddFolderSource,
                                onRemoveFolderSource = onRemoveFolderSource,
                                onClearArtworkCache = onClearArtworkCache,
                                onDefaultFilterSelected = onDefaultFilterSelected,
                                onDefaultSortOrderSelected = onDefaultSortOrderSelected,
                                onCarModeManuallyEnabledChange =
                                    onCarModeManuallyEnabledChange,
                                onCarDeviceMarkedChange = onCarDeviceMarkedChange,
                                onExternalArtworkDownloadEnabledChange =
                                    onExternalArtworkDownloadEnabledChange,
                                onVisualizerPreferredChange = onVisualizerPreferredChange
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
                                    favouriteUris = uiState.favouriteSongs.mapTo(mutableSetOf()) { it.uri },
                                    artworkByUri =
                                        uiState.librarySongs.mapNotNull { song ->
                                            uiState.artworkBySongId[song.id]?.let { song.uri to it }
                                        }.toMap(),
                                    onBack = {
                                        onScreenSelected(playlistEditorReturnDestination)
                                    },
                                    onPlay = onPlayPlaylist,
                                    onMoveEntry = onMovePlaylistEntry,
                                    onFavouriteToggle = onPlaylistEntryFavouriteToggle,
                                    onRemoveEntry = onRemovePlaylistEntry,
                                    onClearPlaylist = onClearPlaylist,
                                    onAddStream = onAddStreamToPlaylist,
                                    onOpenRadioBrowser = onOpenRadioBrowser
                                )
                            }
                        }

                        HomeScreenDestination.RadioBrowser ->
                            RadioStationScreen(
                                uiState = uiState,
                                onBack = {
                                    onScreenSelected(HomeScreenDestination.PlaylistEditor)
                                },
                                onQueryChange = onRadioSearchQueryChange,
                                onSearch = onSearchRadioStations,
                                onPlay = onPlayRadioStation,
                                onAddToPlaylist = onAddRadioStationToPlaylist,
                                onCreatePlaylist = onCreatePlaylist
                            )
                    }
                }
            }
        }
    }
}

private fun HomeScreenDestination.icon(): ImageVector =
    when (this) {
        HomeScreenDestination.Home -> AppIcons.Home
        HomeScreenDestination.NowPlaying -> AppIcons.NowPlaying
        HomeScreenDestination.Playlists -> AppIcons.Playlists
        HomeScreenDestination.Favourites -> AppIcons.Favourite
        HomeScreenDestination.Settings -> AppIcons.Settings
        HomeScreenDestination.PlaylistEditor -> AppIcons.Playlists
        HomeScreenDestination.RadioBrowser -> AppIcons.Playlists
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
    HomeScreenDestination.entries.filterNot {
        it == HomeScreenDestination.PlaylistEditor || it == HomeScreenDestination.RadioBrowser
    }

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

@SuppressLint("MissingPermission")
private fun android.content.Context.isConnectedToCarAudio(): Boolean {
    val bluetoothManager = getSystemService(BluetoothManager::class.java) ?: return false
    val adapter: BluetoothAdapter = bluetoothManager.adapter ?: return false
    return runCatching {
        adapter.getProfileConnectionState(BluetoothProfile.A2DP) ==
                BluetoothAdapter.STATE_CONNECTED
    }
        .getOrDefault(false)
}

@Suppress("DEPRECATION")
private fun Intent.bluetoothDevice(): BluetoothDevice? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }

private fun BluetoothDevice.safeAddress(): String? = runCatching { address }.getOrNull()

@SuppressLint("MissingPermission")
private fun BluetoothDevice.safeName(): String =
    runCatching { name }.getOrNull()?.takeIf(String::isNotBlank) ?: "Bluetooth audio device"
