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
import android.os.BatteryManager
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
import androidx.compose.ui.platform.LocalView
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
    val rootView = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var isCharging by remember { mutableStateOf(false) }
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

    DisposableEffect(context) {
        fun updateChargingState(intent: Intent?) {
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
        }

        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(receiverContext: android.content.Context, intent: Intent) {
                    updateChargingState(intent)
                }
            }
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        updateChargingState(context.registerReceiver(receiver, batteryFilter))
        onDispose { context.unregisterReceiver(receiver) }
    }

    DisposableEffect(
        rootView,
        uiState.isKeepDisplayOnEnabled,
        isCharging,
        uiState.isCarAudioConnected
    ) {
        rootView.keepScreenOn =
            uiState.isKeepDisplayOnEnabled && (isCharging || uiState.isCarAudioConnected)
        onDispose { rootView.keepScreenOn = false }
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

    val playbackActions =
        remember(viewModel) {
            PlaybackActions(
                playPause = viewModel::togglePlayback,
                next = viewModel::skipToNext,
                previous = viewModel::skipToPrevious,
                seekTo = viewModel::updatePlaybackProgress,
                toggleShuffle = viewModel::toggleShuffle,
                cycleRepeatMode = viewModel::cycleRepeatMode,
                setVisualizerEnabled = viewModel::setVisualizerEnabled
            )
        }

    val addFolderSource = { folderLauncher.launch(null) }
    val appActions =
        AppActions(
            navigation =
                NavigationActions(
                    selectScreen = viewModel::selectScreen,
                    requestAudioPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            permissionLauncher.launch(audioPermission)
                        }
                    }
                ),
            library =
                LibraryActions(
                    updateSearch = viewModel::updateSearchQuery,
                    selectFilter = viewModel::selectFilter,
                    selectBrowseValue = viewModel::selectBrowseValue,
                    selectSortOrder = viewModel::selectSortOrder,
                    addFolderSource = addFolderSource
                ),
            songs =
                SongActions(
                    play = viewModel::playSong,
                    playFavourites = viewModel::playFavourites,
                    playQueued = viewModel::playQueuedSong,
                    toggleFavourite = viewModel::toggleFavourite,
                    addToPlaylist = viewModel::addSongToPlaylist,
                    addToQueue = viewModel::addSongToQueue
                ),
            playlists =
                PlaylistFeatureActions(
                    catalog =
                        PlaylistCatalogActions(
                            delete = viewModel::deletePlaylist,
                            create = viewModel::createPlaylist,
                            rename = viewModel::renamePlaylist,
                            duplicate = viewModel::duplicatePlaylist,
                            play = viewModel::playPlaylist,
                            enqueue = viewModel::enqueuePlaylist,
                            clearQueue = viewModel::clearQueue
                        ),
                    documents =
                        PlaylistDocumentActions(
                            import = {
                                playlistImportLauncher.launch(
                                    arrayOf(
                                        "audio/x-mpegurl",
                                        "audio/mpegurl",
                                        "text/plain",
                                        "application/octet-stream"
                                    )
                                )
                            },
                            exportLibrary = {
                                playlistToExport =
                                    M3uPlaylist(
                                        name = "Local Music Library",
                                        entries = uiState.songs.map { it.toM3uEntry() }
                                    )
                                playlistExportLauncher.launch("local-music-library.m3u")
                            },
                            exportPlaylist = { playlist ->
                                playlistToExport = playlist
                                playlistExportLauncher.launch("${playlist.name}.m3u")
                            }
                        ),
                    editor =
                        PlaylistEditorActions(
                            playEntry = viewModel::playPlaylistEntry,
                            moveEntry = viewModel::movePlaylistEntry,
                            toggleEntryFavourite = viewModel::togglePlaylistEntryFavourite,
                            removeEntry = viewModel::removePlaylistEntry,
                            clear = viewModel::clearPlaylist,
                            addStream = viewModel::addStreamToPlaylist,
                            openRadioBrowser = viewModel::openRadioBrowser
                        ),
                    nowPlaying =
                        NowPlayingPlaylistActions(
                            addToPlaylist = viewModel::addNowPlayingToPlaylist,
                            addToQueue = viewModel::addNowPlayingToQueue
                        )
                ),
            radio =
                RadioActions(
                    updateQuery = viewModel::updateRadioSearchQuery,
                    search = viewModel::searchRadioStations,
                    preview = viewModel::previewRadioStation,
                    stopPreview = viewModel::stopRadioPreview,
                    addToPlaylist = viewModel::addRadioStationToPlaylist
                ),
            settings =
                SettingsFeatureActions(
                    folders =
                        FolderActions(
                            add = addFolderSource,
                            remove = { folderUri ->
                                runCatching {
                                    context.contentResolver.releasePersistableUriPermission(
                                        android.net.Uri.parse(folderUri),
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }
                                viewModel.removeFolderSource(folderUri)
                            }
                        ),
                    appearance =
                        AppearanceActions(
                            selectTheme = viewModel::selectThemeMode,
                            clearArtworkCache = viewModel::clearArtworkCache,
                            setExternalArtworkDownloadEnabled =
                                viewModel::setExternalArtworkDownloadEnabled,
                            setVisualizerPreferred = viewModel::setVisualizerPreferred
                        ),
                    preferences =
                        PreferenceActions(
                            setDefaultFilter = viewModel::setDefaultFilter,
                            setDefaultSortOrder = viewModel::setDefaultSortOrder,
                            setCarModeManuallyEnabled = viewModel::setCarModeManuallyEnabled,
                            setKeepDisplayOnEnabled = viewModel::setKeepDisplayOnEnabled,
                            setCarDeviceMarked = viewModel::setCarDeviceMarked
                        )
                )
        )

    HomeScreen(
        uiState = uiState,
        hasAudioPermission = hasAudioPermission,
        playbackActions = playbackActions,
        appActions = appActions
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
internal fun HomeScreen(
    uiState: HomeUiState,
    hasAudioPermission: Boolean,
    playbackActions: PlaybackActions,
    appActions: AppActions
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
        appActions.navigation.selectScreen(HomeScreenDestination.PlaylistEditor)
    }

    BackHandler(enabled = uiState.selectedScreen != HomeScreenDestination.Home) {
        if (uiState.selectedScreen == HomeScreenDestination.RadioBrowser) {
            appActions.radio.stopPreview()
        }
        appActions.navigation.selectScreen(
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
                                appActions.navigation.selectScreen(
                                    HomeScreenDestination.NowPlaying
                                )
                            },
                            playbackActions = playbackActions
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
                            onClick = { appActions.navigation.selectScreen(destination) },
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
                PermissionBanner(
                    onRequestPermission = appActions.navigation.requestAudioPermission
                )
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
                                appActions.navigation.selectScreen(
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
                                libraryActions = appActions.library,
                                songActions = appActions.songs,
                                createPlaylist = appActions.playlists.catalog.create
                            )

                        HomeScreenDestination.NowPlaying ->
                            NowPlayingContent(
                                uiState = uiState,
                                playbackActions = playbackActions,
                                songActions = appActions.songs,
                                playlistActions = appActions.playlists,
                                onShowQueue = {
                                    openPlaylistEditor(
                                        M3uPlaylist.QUEUE_NAME,
                                        HomeScreenDestination.NowPlaying
                                    )
                                }
                            )

                        HomeScreenDestination.Playlists ->
                            PlaylistContent(
                                uiState = uiState,
                                catalogActions = appActions.playlists.catalog,
                                documentActions = appActions.playlists.documents,
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
                                playlists = uiState.importedPlaylists,
                                actions =
                                    appActions.songs.forList(
                                        select = appActions.songs.playFavourites,
                                        createPlaylist = appActions.playlists.catalog.create
                                    )
                            )

                        HomeScreenDestination.Settings ->
                            SettingsContent(
                                uiState = uiState,
                                actions = appActions.settings
                            )

                        HomeScreenDestination.PlaylistEditor -> {
                            val playlist =
                                uiState.importedPlaylists.firstOrNull {
                                    it.name == playlistEditorName
                                }
                            if (playlist == null) {
                                LaunchedEffect(playlistEditorName) {
                                    appActions.navigation.selectScreen(
                                        playlistEditorReturnDestination
                                    )
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
                                        appActions.navigation.selectScreen(
                                            playlistEditorReturnDestination
                                        )
                                    },
                                    catalogActions = appActions.playlists.catalog,
                                    editorActions = appActions.playlists.editor
                                )
                            }
                        }

                        HomeScreenDestination.RadioBrowser ->
                            RadioStationScreen(
                                uiState = uiState,
                                onBack = {
                                    appActions.radio.stopPreview()
                                    appActions.navigation.selectScreen(
                                        HomeScreenDestination.PlaylistEditor
                                    )
                                },
                                actions = appActions.radio,
                                onAddToCurrentPlaylist = { station ->
                                    playlistEditorName?.let { playlistName ->
                                        appActions.radio.addToPlaylist(station, playlistName)
                                    }
                                },
                                createPlaylist = appActions.playlists.catalog.create
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
