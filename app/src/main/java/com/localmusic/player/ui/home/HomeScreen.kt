package com.localmusic.player.ui.home

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.ContextCompat
import com.localmusic.player.bluetooth.CarModeDetector
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.playlist.M3uPlaylist
import com.localmusic.player.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

@Composable
fun HomeRoute(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val bluetoothPermission = Manifest.permission.BLUETOOTH_CONNECT
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.refreshLibraryOnce()
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.updateCarMode(context.isConnectedToCarAudio())
    }
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        folderUri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            folderUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        viewModel.addFolderSource(folderUri.toString())
    }
    val playlistImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { playlistUri ->
        playlistUri ?: return@rememberLauncherForActivityResult
        val name = context.contentResolver.query(playlistUri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        } ?: "Imported Playlist"
        val content = context.contentResolver.openInputStream(playlistUri)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        viewModel.importPlaylist(name, content)
    }
    val playlistExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { playlistUri ->
        playlistUri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(playlistUri)?.bufferedWriter()?.use { writer ->
            writer.write(viewModel.exportCurrentPlaylist())
        }
    }

    LaunchedEffect(hasAudioPermission) {
        yield()
        if (hasAudioPermission) viewModel.refreshLibraryOnce()
    }

    LaunchedEffect(Unit) {
        yield()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasBluetoothPermission = ContextCompat.checkSelfPermission(
                context,
                bluetoothPermission
            ) == PackageManager.PERMISSION_GRANTED
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
        onSortSelected = viewModel::selectSortOrder,
        onAddFolderSource = { folderLauncher.launch(null) },
        onImportPlaylist = { playlistImportLauncher.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "text/plain", "application/octet-stream")) },
        onExportPlaylist = { playlistExportLauncher.launch("local-music-library.m3u") },
        onSongSelected = viewModel::playSong,
        onFavouriteToggle = viewModel::toggleFavourite,
        onDeletePlaylist = viewModel::deletePlaylist,
        onCreatePlaylist = viewModel::createPlaylist,
        onAddNowPlayingToPlaylist = viewModel::addNowPlayingToPlaylist,
        onAddNowPlayingToQueue = viewModel::addNowPlayingToQueue,
        onClearQueue = viewModel::clearQueue,
        onAddSongToPlaylist = viewModel::addSongToPlaylist,
        onAddSongToQueue = viewModel::addSongToQueue,
        onPlayPause = viewModel::togglePlayback,
        onNext = viewModel::skipToNext,
        onPrevious = viewModel::skipToPrevious,
        onProgressChange = viewModel::updatePlaybackProgress,
        onThemeSelected = viewModel::selectThemeMode,
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
    onSortSelected: (SortOrder) -> Unit,
    onAddFolderSource: () -> Unit,
    onImportPlaylist: () -> Unit,
    onExportPlaylist: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onFavouriteToggle: (Song) -> Unit,
    onDeletePlaylist: (M3uPlaylist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onAddNowPlayingToPlaylist: (String) -> Unit,
    onAddNowPlayingToQueue: () -> Unit,
    onClearQueue: () -> Unit,
    onAddSongToPlaylist: (Song, String) -> Unit,
    onAddSongToQueue: (Song) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onThemeSelected: (AppThemeMode) -> Unit,
    onRequestPermission: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Local Music",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                HomeScreenDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = uiState.selectedScreen == destination,
                        onClick = { onScreenSelected(destination) },
                        icon = {
                            Text(
                                text = destination.iconLabel(),
                                fontSize = 28.sp
                            )
                        },
                        label = null
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
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
                Text(text = "Refreshing local music...", style = MaterialTheme.typography.bodyMedium)
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
            if (uiState.selectedScreen == HomeScreenDestination.Home) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAddFolderSource) {
                        Text("Add Folder")
                    }
                    Button(onClick = onImportPlaylist) {
                        Text("Import M3U")
                    }
                    Button(onClick = onExportPlaylist) {
                        Text("Export M3U")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                FilterRow(uiState, onFilterSelected, onSortSelected)
                Spacer(modifier = Modifier.height(12.dp))
                if (uiState.importedPlaylists.isNotEmpty()) {
                    Text(
                        text = "Imported playlists: ${uiState.importedPlaylists.joinToString { it.name }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            when (uiState.selectedScreen) {
                HomeScreenDestination.Home -> AdaptiveLibraryContent(
                    uiState = uiState,
                    onSongSelected = onSongSelected,
                    onFavouriteToggle = onFavouriteToggle,
                    onCreatePlaylist = onCreatePlaylist,
                    onAddSongToPlaylist = onAddSongToPlaylist,
                    onAddSongToQueue = onAddSongToQueue
                )
                HomeScreenDestination.NowPlaying -> NowPlayingContent(
                    uiState = uiState,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onProgressChange = onProgressChange,
                    onCreatePlaylist = onCreatePlaylist,
                    onAddToPlaylist = onAddNowPlayingToPlaylist,
                    onAddToQueue = onAddNowPlayingToQueue
                )
                HomeScreenDestination.Playlists -> PlaylistContent(
                    uiState = uiState,
                    onDeletePlaylist = onDeletePlaylist,
                    onClearQueue = onClearQueue
                )
                HomeScreenDestination.Favourites -> SongList(
                    songs = uiState.songs.filter { it.isFavourite },
                    artworkBySongId = uiState.artworkBySongId,
                    emptyTitle = "No favourites yet",
                    emptyMessage = "Mark local songs as favourites to pin them here.",
                    onSongSelected = onSongSelected,
                    onFavouriteToggle = onFavouriteToggle,
                    playlists = uiState.importedPlaylists,
                    onCreatePlaylist = onCreatePlaylist,
                    onAddSongToPlaylist = onAddSongToPlaylist,
                    onAddSongToQueue = onAddSongToQueue
                )
                HomeScreenDestination.Settings -> SettingsContent(
                    uiState = uiState,
                    onThemeSelected = onThemeSelected
                )
            }
        }
    }
}

private fun HomeScreenDestination.iconLabel(): String = when (this) {
    HomeScreenDestination.Home -> "⌂"
    HomeScreenDestination.NowPlaying -> "▶"
    HomeScreenDestination.Playlists -> "≡"
    HomeScreenDestination.Favourites -> "★"
    HomeScreenDestination.Settings -> "⚙"
}

@Composable
private fun PermissionBanner(onRequestPermission: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Allow audio access to index local songs.",
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onRequestPermission) {
            Text("Allow Audio Access")
        }
    }
}

@Composable
private fun FilterRow(
    uiState: HomeUiState,
    onFilterSelected: (LibraryFilter) -> Unit,
    onSortSelected: (SortOrder) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryFilter.entries.forEach { filter ->
                AssistChip(
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) },
                    enabled = filter != uiState.selectedFilter
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortOrder.entries.forEach { sortOrder ->
                AssistChip(
                    onClick = { onSortSelected(sortOrder) },
                    label = { Text(sortOrder.label) },
                    enabled = sortOrder != uiState.sortOrder
                )
            }
        }
    }
}

@Composable
private fun AdaptiveLibraryContent(
    uiState: HomeUiState,
    onSongSelected: (Song) -> Unit,
    onFavouriteToggle: (Song) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onAddSongToPlaylist: (Song, String) -> Unit,
    onAddSongToQueue: (Song) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SongList(
                    songs = uiState.songs,
                    artworkBySongId = uiState.artworkBySongId,
                    modifier = Modifier.weight(1f),
                    onSongSelected = onSongSelected,
                    onFavouriteToggle = onFavouriteToggle,
                    playlists = uiState.importedPlaylists,
                    onCreatePlaylist = onCreatePlaylist,
                    onAddSongToPlaylist = onAddSongToPlaylist,
                    onAddSongToQueue = onAddSongToQueue
                )
                SongList(
                    songs = uiState.songs.filter { it.isFavourite },
                    artworkBySongId = uiState.artworkBySongId,
                    emptyTitle = "No favourites yet",
                    emptyMessage = "Mark local songs as favourites to pin them here.",
                    modifier = Modifier.width(320.dp),
                    onSongSelected = onSongSelected,
                    onFavouriteToggle = onFavouriteToggle,
                    playlists = uiState.importedPlaylists,
                    onCreatePlaylist = onCreatePlaylist,
                    onAddSongToPlaylist = onAddSongToPlaylist,
                    onAddSongToQueue = onAddSongToQueue
                )
            }
        } else {
            SongList(
                songs = uiState.songs,
                artworkBySongId = uiState.artworkBySongId,
                onSongSelected = onSongSelected,
                onFavouriteToggle = onFavouriteToggle,
                playlists = uiState.importedPlaylists,
                onCreatePlaylist = onCreatePlaylist,
                onAddSongToPlaylist = onAddSongToPlaylist,
                onAddSongToQueue = onAddSongToQueue
            )
        }
    }
}

@Composable
private fun NowPlayingContent(
    uiState: HomeUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onAddToQueue: () -> Unit
) {
    val song = uiState.nowPlayingSong
    var showPlaylistChooser by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    if (song == null) {
        EmptyState(
            title = "Nothing playing",
            message = "Choose a song from Home or Favourites to start playback."
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ArtworkBackdrop(artworkUri = uiState.artworkBySongId[song.id])
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ArtworkThumbnail(artworkUri = uiState.artworkBySongId[song.id])
            Text(text = song.title, style = MaterialTheme.typography.headlineMedium)
            Text(text = "${song.artist} - ${song.album}", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = uiState.playbackProgress,
                onValueChange = onProgressChange
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPrevious) { Text("Previous") }
                Button(onClick = onPlayPause) { Text(if (uiState.isPlaying) "Pause" else "Play") }
                Button(onClick = onNext) { Text("Next") }
            }
            Button(onClick = { showPlaylistChooser = true }) {
                Text("Add to playlist")
            }
            Button(onClick = onAddToQueue) {
                Text("Add to queue")
            }
        }
    }

    if (showPlaylistChooser) {
        AlertDialog(
            onDismissRequest = { showPlaylistChooser = false },
            title = { Text("Add to playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.importedPlaylists.isEmpty()) {
                        Text("Create a playlist to save this song.")
                    } else {
                        uiState.importedPlaylists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    onAddToPlaylist(playlist.name)
                                    showPlaylistChooser = false
                                }
                            ) {
                                Text(playlist.name)
                            }
                        }
                    }
                    Button(onClick = {
                        showPlaylistChooser = false
                        showCreatePlaylistDialog = true
                    }) {
                        Text("New playlist")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistChooser = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                onCreatePlaylist(name)
                onAddToPlaylist(name.trim())
                showCreatePlaylistDialog = false
            }
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                singleLine = true,
                label = { Text("Playlist name") }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(playlistName) },
                enabled = playlistName.isNotBlank()
            ) { Text("Create") }
        }
    )
}

@Composable
private fun PlaylistContent(
    uiState: HomeUiState,
    onDeletePlaylist: (M3uPlaylist) -> Unit,
    onClearQueue: () -> Unit
) {
    if (uiState.importedPlaylists.isEmpty()) {
        EmptyState(
            title = "No playlists imported",
            message = "Use Import M3U on Home to add local playlist files."
        )
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(uiState.importedPlaylists) { playlist ->
            ListItem(
                headlineContent = { Text(playlist.name) },
                supportingContent = { Text("${playlist.entries.size} entries") },
                trailingContent = {
                    if (playlist.name == M3uPlaylist.QUEUE_NAME) {
                        Button(onClick = onClearQueue) {
                            Text("Clear")
                        }
                    } else {
                        Button(onClick = { onDeletePlaylist(playlist) }) {
                            Text("Delete")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: HomeUiState,
    onThemeSelected: (AppThemeMode) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Theme", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { mode ->
                AssistChip(
                    onClick = { onThemeSelected(mode) },
                    label = { Text(mode.label) },
                    enabled = mode != uiState.themeMode
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    artworkBySongId: Map<String, String>,
    modifier: Modifier = Modifier.fillMaxSize(),
    onSongSelected: (Song) -> Unit,
    onFavouriteToggle: (Song) -> Unit,
    playlists: List<M3uPlaylist>,
    onCreatePlaylist: (String) -> Unit,
    onAddSongToPlaylist: (Song, String) -> Unit,
    onAddSongToQueue: (Song) -> Unit
) {
    SongList(
        songs = songs,
        artworkBySongId = artworkBySongId,
        emptyTitle = "No local songs indexed yet",
        emptyMessage = "Allow audio access to scan MediaStore, or add a folder source.",
        modifier = modifier,
        onSongSelected = onSongSelected,
        onFavouriteToggle = onFavouriteToggle,
        playlists = playlists,
        onCreatePlaylist = onCreatePlaylist,
        onAddSongToPlaylist = onAddSongToPlaylist,
        onAddSongToQueue = onAddSongToQueue
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongList(
    songs: List<Song>,
    artworkBySongId: Map<String, String>,
    emptyTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    onSongSelected: (Song) -> Unit,
    onFavouriteToggle: (Song) -> Unit,
    playlists: List<M3uPlaylist>,
    onCreatePlaylist: (String) -> Unit,
    onAddSongToPlaylist: (Song, String) -> Unit,
    onAddSongToQueue: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyState(title = emptyTitle, message = emptyMessage, modifier = modifier)
        return
    }

    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showPlaylistChooser by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items = songs, key = { it.id }) { song ->
            ListItem(
                modifier = Modifier.combinedClickable(
                    onClick = { onSongSelected(song) },
                    onLongClick = {
                        selectedSong = song
                        showActionMenu = true
                    }
                ),
                leadingContent = { ArtworkThumbnail(artworkUri = artworkBySongId[song.id]) },
                headlineContent = { Text(song.title) },
                supportingContent = { Text("${song.artist} - ${song.album}") },
                trailingContent = {
                    IconButton(onClick = { onFavouriteToggle(song) }) {
                        Text(if (song.isFavourite) "★" else "☆")
                    }
                }
            )
        }
    }

    if (showActionMenu) selectedSong?.let { song ->
        AlertDialog(
            onDismissRequest = {
                selectedSong = null
                showActionMenu = false
            },
            title = { Text(song.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        onAddSongToQueue(song)
                        selectedSong = null
                        showActionMenu = false
                    }) { Text("Add to queue") }
                    TextButton(onClick = {
                        showActionMenu = false
                        showPlaylistChooser = true
                    }) {
                        Text("Add to playlist")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedSong = null
                    showActionMenu = false
                }) { Text("Cancel") }
            }
        )
    }

    if (showPlaylistChooser) {
        AlertDialog(
            onDismissRequest = {
                showPlaylistChooser = false
                selectedSong = null
            },
            title = { Text("Add to playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    playlists.filterNot { it.name == M3uPlaylist.QUEUE_NAME }.forEach { playlist ->
                        TextButton(onClick = {
                            selectedSong?.let { song -> onAddSongToPlaylist(song, playlist.name) }
                            selectedSong = null
                            showPlaylistChooser = false
                        }) { Text(playlist.name) }
                    }
                    Button(onClick = {
                        showPlaylistChooser = false
                        showCreatePlaylistDialog = true
                    }) { Text("New playlist") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPlaylistChooser = false
                    selectedSong = null
                }) { Text("Cancel") }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = {
                selectedSong = null
                showCreatePlaylistDialog = false
            },
            onCreate = { name ->
                val normalizedName = name.trim()
                onCreatePlaylist(normalizedName)
                selectedSong?.let { song -> onAddSongToPlaylist(song, normalizedName) }
                selectedSong = null
                showCreatePlaylistDialog = false
            }
        )
    }
}

@Composable
private fun ArtworkThumbnail(artworkUri: String?) {
    val context = LocalContext.current
    var bitmap by remember(artworkUri) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(artworkUri) {
        bitmap = null
        if (artworkUri != null) {
            bitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(Uri.parse(artworkUri))?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
        }
    }

    val thumbnail = bitmap
    if (thumbnail != null) {
        Image(
            bitmap = thumbnail.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Crop
        )
    } else {
        Text(
            text = "♪",
            modifier = Modifier.size(48.dp),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun ArtworkBackdrop(artworkUri: String?) {
    val context = LocalContext.current
    var bitmap by remember(artworkUri) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(artworkUri) {
        bitmap = artworkUri?.let { uri ->
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use(BitmapFactory::decodeStream)
            }
        }
    }

    bitmap?.let { artwork ->
        Image(
            bitmap = artwork.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.16f),
            contentScale = ContentScale.Crop
        )
    }
}

private fun android.content.Context.isConnectedToCarAudio(): Boolean {
    val bluetoothManager = getSystemService(BluetoothManager::class.java) ?: return false
    val adapter: BluetoothAdapter = bluetoothManager.adapter ?: return false
    val detector = CarModeDetector()
    return runCatching { adapter.bondedDevices.any(detector::isLikelyCarDevice) }.getOrDefault(false)
}
