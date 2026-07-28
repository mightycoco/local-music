package com.localmusic.player.ui.home

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localmusic.player.playlist.M3uPlaylist
import kotlin.math.roundToInt

@Composable
internal fun PlaylistContent(
        uiState: HomeUiState,
        onDeletePlaylist: (M3uPlaylist) -> Unit,
        onRenamePlaylist: (M3uPlaylist, String) -> Unit,
        onDuplicatePlaylist: (M3uPlaylist, String) -> Unit,
        onPlayPlaylist: (M3uPlaylist) -> Unit,
        onExportPlaylist: (M3uPlaylist) -> Unit,
        onClearQueue: () -> Unit,
        onOpenPlaylistEditor: (M3uPlaylist) -> Unit
) {
    var playlistToRename by remember { mutableStateOf<M3uPlaylist?>(null) }
    var playlistToDuplicate by remember { mutableStateOf<M3uPlaylist?>(null) }

    if (uiState.importedPlaylists.isEmpty()) {
        EmptyState(
                title = "No playlists imported",
                message = "Use Import M3U on Home to add local playlist files."
        )
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(uiState.importedPlaylists) { playlist ->
            var showActions by remember(playlist.name) { mutableStateOf(false) }
            ListItem(
                    modifier = Modifier.clickable { onOpenPlaylistEditor(playlist) },
                    headlineContent = { Text(playlist.name) },
                    supportingContent = { Text("${playlist.entries.size} entries") },
                    trailingContent = {
                        Box {
                            IconButton(onClick = { showActions = true }) {
                                Text(Glyphs.MORE.glyph, fontSize = 24.sp)
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

@Composable
internal fun PlaylistEditorContent(
        playlist: M3uPlaylist,
        onBack: () -> Unit,
        onMoveEntry: (M3uPlaylist, Int, Int) -> Unit,
        onClearPlaylist: (M3uPlaylist) -> Unit,
        onDeletePlaylist: (M3uPlaylist) -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
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
            IconButton(onClick = onBack) { Text(Glyphs.HOME.glyph, fontSize = 26.sp) }
            Text(playlist.name, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            Spacer(modifier = Modifier.size(48.dp))
        }
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                    onClick = { showClearConfirmation = true },
                    enabled = playlist.entries.isNotEmpty()
            ) { Text("Clear") }
            if (playlist.name != M3uPlaylist.QUEUE_NAME) {
                Button(onClick = { showDeleteConfirmation = true }) { Text("Delete") }
            }
        }
        if (playlist.entries.isEmpty()) {
            EmptyState(title = "No entries", message = "Add songs to start this list.")
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(playlistRowSpacing)
            ) {
                items(playlist.entries.size, key = { playlist.entries[it].uri }) { index ->
                    val entry = playlist.entries[index]
                    var previousIndex by remember { mutableIntStateOf(index) }
                    val isDragged = entry.uri == draggedEntryUri
                    val placementOffset by
                            animateIntAsState(
                                    targetValue =
                                            if (isDragged || previousIndex == index) 0
                                            else (previousIndex - index) * rowPitchPx.roundToInt(),
                                    label = "playlist-entry-placement"
                            )
                    LaunchedEffect(index) { previousIndex = index }
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .height(playlistRowHeight)
                                            .padding(horizontal = 16.dp)
                                            .graphicsLayer {
                                                translationY =
                                                        if (isDragged) dragOffset
                                                        else placementOffset.toFloat()
                                            },
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, maxLines = 1)
                            Text(
                                    entry.artist,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall
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
                        ) { Text(text = Glyphs.REORDER.glyph, fontSize = 22.sp) }
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
    if (showDeleteConfirmation) {
        ConfirmPlaylistActionDialog(
                title = "Delete ${playlist.name}?",
                message = "This deletes the playlist and its contents.",
                confirmLabel = "Delete",
                onDismiss = { showDeleteConfirmation = false },
                onConfirm = {
                    onDeletePlaylist(playlist)
                    showDeleteConfirmation = false
                    onBack()
                }
        )
    }
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
