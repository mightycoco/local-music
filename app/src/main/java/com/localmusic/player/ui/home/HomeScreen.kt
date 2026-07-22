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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
        if (granted) viewModel.refreshLibrary()
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
        if (hasAudioPermission) viewModel.refreshLibrary()
    }

    LaunchedEffect(Unit) {
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
        onFilterSelected = viewModel::selectFilter,
        onSortSelected = viewModel::selectSortOrder,
        onAddFolderSource = { folderLauncher.launch(null) },
        onImportPlaylist = { playlistImportLauncher.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "text/plain", "application/octet-stream")) },
        onExportPlaylist = { playlistExportLauncher.launch("local-music-library.m3u") },
        onSongSelected = viewModel::playSong,
        onFavouriteToggle = viewModel::toggleFavourite,
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
    onFilterSelected: (LibraryFilter) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onAddFolderSource: () -> Unit,
    onImportPlaylist: () -> Unit,
    onExportPlaylist: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onFavouriteToggle: (Song) -> Unit,
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search") }
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (!hasAudioPermission) {
                PermissionBanner(onRequestPermission = onRequestPermission)
                Spacer(modifier = Modifier.height(12.dp))
            }
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
            FilterRow(uiState, onFilterSelected, onSortSelected)
            Spacer(modifier = Modifier.height(12.dp))
            if (uiState.importedPlaylists.isNotEmpty()) {
                Text(
                    text = "Imported playlists: ${uiState.importedPlaylists.joinToString { it.name }}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            AdaptiveLibraryContent(
                uiState = uiState,
                onSongSelected = onSongSelected,
                onFavouriteToggle = onFavouriteToggle
            )
        }
    }
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
    onFavouriteToggle: (Song) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SongList(
                    songs = uiState.songs,
                    artworkBySongId = uiState.artworkBySongId,
                    modifier = Modifier.weight(1f),
                    onSongSelected = onSongSelected,
                    onFavouriteToggle = onFavouriteToggle
                )
                SongList(
                    songs = uiState.songs.filter { it.isFavourite },
                    artworkBySongId = uiState.artworkBySongId,
                    emptyTitle = "No favourites yet",
                    emptyMessage = "Mark local songs as favourites to pin them here.",
                    modifier = Modifier.width(320.dp),
                    onSongSelected = onSongSelected,
                    onFavouriteToggle = onFavouriteToggle
                )
            }
        } else {
            SongList(
                songs = uiState.songs,
                artworkBySongId = uiState.artworkBySongId,
                onSongSelected = onSongSelected,
                onFavouriteToggle = onFavouriteToggle
            )
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    artworkBySongId: Map<String, String>,
    modifier: Modifier = Modifier.fillMaxSize(),
    onSongSelected: (Song) -> Unit,
    onFavouriteToggle: (Song) -> Unit
) {
    SongList(
        songs = songs,
        artworkBySongId = artworkBySongId,
        emptyTitle = "No local songs indexed yet",
        emptyMessage = "Allow audio access to scan MediaStore, or add a folder source.",
        modifier = modifier,
        onSongSelected = onSongSelected,
        onFavouriteToggle = onFavouriteToggle
    )
}

@Composable
private fun SongList(
    songs: List<Song>,
    artworkBySongId: Map<String, String>,
    emptyTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    onSongSelected: (Song) -> Unit,
    onFavouriteToggle: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        Column(
            modifier = modifier
                .padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = emptyTitle,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items = songs, key = { it.id }) { song ->
            ListItem(
                modifier = Modifier.clickable { onSongSelected(song) },
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
}

@Composable
private fun ArtworkThumbnail(artworkUri: String?) {
    val context = LocalContext.current
    val bitmap = remember(artworkUri) {
        artworkUri?.let { uri ->
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
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

private fun android.content.Context.isConnectedToCarAudio(): Boolean {
    val bluetoothManager = getSystemService(BluetoothManager::class.java) ?: return false
    val adapter: BluetoothAdapter = bluetoothManager.adapter ?: return false
    val detector = CarModeDetector()
    return runCatching { adapter.bondedDevices.any(detector::isLikelyCarDevice) }.getOrDefault(false)
}
