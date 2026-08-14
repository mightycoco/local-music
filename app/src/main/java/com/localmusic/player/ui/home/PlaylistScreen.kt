package com.localmusic.player.ui.home

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localmusic.player.playlist.M3uPlaylist
import com.localmusic.player.playlist.M3uPlaylistEntry
import com.localmusic.player.ui.theme.UiAnimationTimings
import kotlin.math.roundToInt

@Composable
internal fun PlaylistContent(
    uiState: HomeUiState,
    onDeletePlaylist: (M3uPlaylist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (M3uPlaylist, String) -> Unit,
    onDuplicatePlaylist: (M3uPlaylist, String) -> Unit,
    onPlayPlaylist: (M3uPlaylist) -> Unit,
    onImportPlaylist: () -> Unit,
    onExportLibrary: () -> Unit,
    onExportPlaylist: (M3uPlaylist) -> Unit,
    onClearQueue: () -> Unit,
    onOpenPlaylistEditor: (M3uPlaylist) -> Unit
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<M3uPlaylist?>(null) }
    var playlistToDuplicate by remember { mutableStateOf<M3uPlaylist?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { showCreatePlaylistDialog = true },
                modifier = Modifier.semantics { contentDescription = "New playlist" }
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            }
            IconButton(
                onClick = onImportPlaylist,
                modifier = Modifier.semantics { contentDescription = "Import M3U" }
            ) {
                Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null)
            }
            IconButton(
                onClick = onExportLibrary,
                modifier = Modifier.semantics { contentDescription = "Export M3U" }
            ) {
                Icon(imageVector = Icons.Outlined.FileUpload, contentDescription = null)
            }
        }

        val visiblePlaylists =
            uiState.importedPlaylists.filterNot { it.name == M3uPlaylist.ONLINE_FAVOURITES_NAME }
        if (visiblePlaylists.isEmpty()) {
            EmptyState(
                title = "No playlists imported",
                message = "Import a local M3U file to add a playlist."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visiblePlaylists) { playlist ->
                    var showActions by remember(playlist.name) { mutableStateOf(false) }
                    ListItem(
                        modifier = Modifier.clickable { onOpenPlaylistEditor(playlist) },
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text("${playlist.entries.size} entries") },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { showActions = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = "Playlist actions"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showActions,
                                    onDismissRequest = { showActions = false }
                                ) {
                                    if (playlist.name == M3uPlaylist.QUEUE_NAME) {
                                        DropdownMenuItem(
                                            text = { Text("Clear") },
                                            onClick = {
                                                onClearQueue()
                                                showActions = false
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("Play") },
                                            enabled = playlist.entries.isNotEmpty(),
                                            onClick = {
                                                onPlayPlaylist(playlist)
                                                showActions = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Rename") },
                                            onClick = {
                                                playlistToRename = playlist
                                                showActions = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Duplicate") },
                                            onClick = {
                                                playlistToDuplicate = playlist
                                                showActions = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Export") },
                                            onClick = {
                                                onExportPlaylist(playlist)
                                                showActions = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete") },
                                            onClick = {
                                                onDeletePlaylist(playlist)
                                                showActions = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                onCreatePlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    playlistToRename?.let { playlist ->
        PlaylistNameDialog(
            title = "Rename playlist",
            initialName = playlist.name,
            confirmLabel = "Rename",
            onDismiss = { playlistToRename = null },
            onConfirm = { name ->
                onRenamePlaylist(playlist, name)
                playlistToRename = null
            }
        )
    }

    playlistToDuplicate?.let { playlist ->
        PlaylistNameDialog(
            title = "Duplicate playlist",
            initialName = "${playlist.name} copy",
            confirmLabel = "Duplicate",
            onDismiss = { playlistToDuplicate = null },
            onConfirm = { name ->
                onDuplicatePlaylist(playlist, name)
                playlistToDuplicate = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlaylistEditorContent(
    playlist: M3uPlaylist,
    favouriteUris: Set<String>,
    artworkByUri: Map<String, String>,
    onBack: () -> Unit,
    onPlay: (M3uPlaylist) -> Unit,
    onPlayEntry: (M3uPlaylist, M3uPlaylistEntry) -> Unit,
    onMoveEntry: (M3uPlaylist, Int, Int) -> Unit,
    onFavouriteToggle: (M3uPlaylistEntry) -> Unit,
    onRemoveEntry: (M3uPlaylist, Int) -> Unit,
    onClearPlaylist: (M3uPlaylist) -> Unit,
    onAddStream: (M3uPlaylist, String) -> Unit,
    onOpenRadioBrowser: () -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showAddStreamDialog by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var entryToRemove by remember { mutableStateOf<Int?>(null) }
    var draggedEntryUri by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val playlistRowHeight = 64.dp
    val playlistRowSpacing = 4.dp
    val rowPitchPx = with(LocalDensity.current) { (playlistRowHeight + playlistRowSpacing).toPx() }
    val currentPlaylist by rememberUpdatedState(playlist)
    val currentOnMoveEntry by rememberUpdatedState(onMoveEntry)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(playlist.name, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            Spacer(modifier = Modifier.size(48.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onPlay(playlist) },
                enabled = playlist.entries.isNotEmpty()
            ) { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play") }
            Box {
                IconButton(
                    onClick = { showAddMenu = true },
                    enabled = playlist.name != M3uPlaylist.QUEUE_NAME,
                    modifier = Modifier.semantics { contentDescription = "Add to playlist" }
                ) { Text("+", fontSize = 28.sp) }
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Add Stream") },
                        onClick = {
                            showAddMenu = false
                            showAddStreamDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add Radio Station") },
                        onClick = {
                            showAddMenu = false
                            onOpenRadioBrowser()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = { showClearConfirmation = true },
                enabled = playlist.entries.isNotEmpty(),
                modifier = Modifier.size(48.dp).semantics { contentDescription = "Clear playlist" },
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = null)
            }
        }

        if (playlist.entries.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No entries",
                    message = "Add songs or a stream to start this list."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(playlistRowSpacing)
            ) {
                items(playlist.entries.size, key = { index -> playlist.entries[index].uri }) { index ->
                    val entry = playlist.entries[index]
                    var previousIndex by remember { mutableIntStateOf(index) }
                    val isDragged = entry.uri == draggedEntryUri
                    val placementOffset by
                    animateIntAsState(
                        targetValue =
                            if (isDragged || previousIndex == index) 0
                            else (previousIndex - index) * rowPitchPx.roundToInt(),
                        animationSpec =
                            tween(UiAnimationTimings.PLAYLIST_REORDER_MILLIS),
                        label = "playlist-entry-placement"
                    )
                    LaunchedEffect(index) { previousIndex = index }
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(playlistRowHeight)
                                .padding(horizontal = 16.dp)
                                .combinedClickable(
                                    onClick = { onPlayEntry(playlist, entry) },
                                    onLongClick = { entryToRemove = index }
                                )
                                .graphicsLayer {
                                    translationY =
                                        if (isDragged) dragOffset
                                        else placementOffset.toFloat()
                                },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtworkThumbnail(
                            artworkUri = artworkByUri[entry.uri],
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, maxLines = 1)
                            Text(
                                entry.artist,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(
                            onClick = { onFavouriteToggle(entry) },
                            modifier = Modifier.semantics {
                                contentDescription =
                                    if (entry.uri in favouriteUris) {
                                        "Remove from favourites"
                                    } else {
                                        "Add to favourites"
                                    }
                            }
                        ) {
                            Icon(
                                imageVector =
                                    if (entry.uri in favouriteUris) Icons.Filled.Favorite
                                    else Icons.Outlined.FavoriteBorder,
                                contentDescription = null
                            )
                        }
                        Box(
                            modifier =
                                Modifier.size(48.dp).pointerInput(entry.uri) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            if (currentPlaylist.entries.none {
                                                    it.uri == entry.uri
                                                }
                                            )
                                                return@detectVerticalDragGestures
                                            draggedEntryUri = entry.uri
                                            dragOffset = 0f
                                        },
                                        onVerticalDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount
                                            val draggedUri =
                                                draggedEntryUri
                                                    ?: return@detectVerticalDragGestures
                                            val currentIndex =
                                                currentPlaylist.entries
                                                    .indexOfFirst {
                                                        it.uri == draggedUri
                                                    }
                                            if (currentIndex < 0) {
                                                return@detectVerticalDragGestures
                                            }
                                            if (dragOffset > rowPitchPx / 2f &&
                                                currentIndex <
                                                currentPlaylist
                                                    .entries
                                                    .lastIndex
                                            ) {
                                                currentOnMoveEntry(
                                                    currentPlaylist,
                                                    currentIndex,
                                                    1
                                                )
                                                dragOffset -= rowPitchPx
                                            } else if (dragOffset < -rowPitchPx / 2f &&
                                                currentIndex > 0
                                            ) {
                                                currentOnMoveEntry(
                                                    currentPlaylist,
                                                    currentIndex,
                                                    -1
                                                )
                                                dragOffset += rowPitchPx
                                            }
                                        },
                                        onDragEnd = {
                                            draggedEntryUri = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggedEntryUri = null
                                            dragOffset = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DragHandle,
                                contentDescription = "Reorder"
                            )
                        }
                        DropdownMenu(
                            expanded = entryToRemove == index,
                            onDismissRequest = { entryToRemove = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Remove from Playlist") },
                                onClick = {
                                    onRemoveEntry(playlist, index)
                                    entryToRemove = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel") },
                                onClick = { entryToRemove = null }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        ConfirmPlaylistActionDialog(
            title = "Clear ${playlist.name}?",
            message = "This removes every song from this list.",
            confirmLabel = "Clear",
            onDismiss = { showClearConfirmation = false },
            onConfirm = {
                onClearPlaylist(playlist)
                showClearConfirmation = false
            }
        )
    }
    if (showAddStreamDialog) {
        AddStreamDialog(
            onDismiss = { showAddStreamDialog = false },
            onConfirm = { streamUrl ->
                onAddStream(playlist, streamUrl)
                showAddStreamDialog = false
            }
        )
    }
}

@Composable
private fun AddStreamDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var streamUrl by remember { mutableStateOf("") }
    val normalizedUrl = streamUrl.trim()
    val isHttpUrl =
        normalizedUrl.startsWith("http://", ignoreCase = true) ||
                normalizedUrl.startsWith("https://", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Stream") },
        text = {
            OutlinedTextField(
                value = streamUrl,
                onValueChange = { streamUrl = it },
                singleLine = true,
                label = { Text("HTTP(S) stream or M3U URL") }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(onClick = { onConfirm(normalizedUrl) }, enabled = isHttpUrl) {
                Text("Add")
            }
        }
    )
}

@Composable
private fun ConfirmPlaylistActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } }
    )
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var playlistName by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                singleLine = true,
                label = { Text("Playlist name") }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(playlistName) },
                enabled = playlistName.isNotBlank()
            ) { Text(confirmLabel) }
        }
    )
}
