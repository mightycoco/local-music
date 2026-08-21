package com.localmusic.player.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.localmusic.player.domain.model.Song
import com.localmusic.player.playlist.M3uPlaylist
import kotlinx.coroutines.delay

@Composable
internal fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(
                onClick = { onCreate(playlistName) },
                enabled = playlistName.isNotBlank()
            ) { Text("Create") }
        }
    )
}

@Composable
internal fun PlaylistChooserDialog(
    playlists: List<M3uPlaylist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                onCreatePlaylist(name.trim())
                showCreatePlaylistDialog = false
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                playlists.filterNot {
                    it.name == M3uPlaylist.QUEUE_NAME ||
                            it.name == M3uPlaylist.ONLINE_FAVOURITES_NAME
                }.forEach { playlist ->
                    TextButton(onClick = { onPlaylistSelected(playlist.name) }) {
                        Text(playlist.name)
                    }
                }
                Button(onClick = { showCreatePlaylistDialog = true }) { Text("New playlist") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun SongList(
    songs: List<Song>,
    artworkBySongId: Map<String, String>,
    nowPlayingSongId: String?,
    isPlaying: Boolean,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier.fillMaxSize(),
    playlists: List<M3uPlaylist>,
    onLoadNextPage: (() -> Unit)? = null,
    actions: SongListActions
) {
    SongList(
        songs = songs,
        artworkBySongId = artworkBySongId,
        nowPlayingSongId = nowPlayingSongId,
        isPlaying = isPlaying,
        listState = listState,
        emptyTitle = "No local songs indexed yet",
        emptyMessage = "Allow audio access to scan MediaStore, or add a folder source.",
        modifier = modifier,
        playlists = playlists,
        onLoadNextPage = onLoadNextPage,
        actions = actions
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SongList(
    songs: List<Song>,
    artworkBySongId: Map<String, String>,
    nowPlayingSongId: String?,
    isPlaying: Boolean,
    listState: LazyListState = rememberLazyListState(),
    emptyTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    playlists: List<M3uPlaylist>,
    onLoadNextPage: (() -> Unit)? = null,
    actions: SongListActions
) {
    if (songs.isEmpty()) {
        EmptyState(title = emptyTitle, message = emptyMessage, modifier = modifier)
        return
    }

    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showPlaylistChooser by remember { mutableStateOf(false) }

    LaunchedEffect(listState, songs.size, onLoadNextPage) {
        if (onLoadNextPage == null) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collectLatest { lastVisibleIndex ->
                if (lastVisibleIndex >= songs.lastIndex - PAGE_LOAD_AHEAD_ITEMS) onLoadNextPage()
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items = songs, key = { it.id }) { song ->
            ListItem(
                modifier =
                    Modifier.combinedClickable(
                        onClick = { actions.select(song) },
                        onLongClick = {
                            selectedSong = song
                            showActionMenu = true
                        }
                    ),
                leadingContent = { ArtworkThumbnail(artworkUri = artworkBySongId[song.id]) },
                headlineContent = {
                    PlayingSongTitle(
                        title = song.title,
                        isCurrent = song.id == nowPlayingSongId,
                        isPlaying = isPlaying
                    )
                },
                supportingContent = { Text("${song.artist} - ${song.album}") },
                trailingContent = {
                    IconButton(onClick = { actions.toggleFavourite(song) }) {
                        Icon(
                            imageVector =
                                if (song.isFavourite) Icons.Filled.Favorite
                                else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                        )
                    }
                }
            )
        }
    }

    if (showActionMenu) {
        selectedSong?.let { song ->
            AlertDialog(
                onDismissRequest = {
                    selectedSong = null
                    showActionMenu = false
                },
                title = { Text(song.title) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                actions.addToQueue(song)
                                selectedSong = null
                                showActionMenu = false
                            }
                        ) { Text("Add to queue") }
                        TextButton(
                            onClick = {
                                showActionMenu = false
                                showPlaylistChooser = true
                            }
                        ) { Text("Add to playlist") }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedSong = null
                            showActionMenu = false
                        }
                    ) { Text("Cancel") }
                }
            )
        }
    }

    if (showPlaylistChooser) {
        PlaylistChooserDialog(
            playlists = playlists,
            onDismiss = {
                showPlaylistChooser = false
                selectedSong = null
            },
            onPlaylistSelected = { playlistName ->
                selectedSong?.let { song -> actions.addToPlaylist(song, playlistName) }
                selectedSong = null
                showPlaylistChooser = false
            },
            onCreatePlaylist = { name ->
                actions.createPlaylist(name)
                selectedSong?.let { song -> actions.addToPlaylist(song, name) }
                selectedSong = null
                showPlaylistChooser = false
            }
        )
    }
}

private const val PAGE_LOAD_AHEAD_ITEMS = 20

@Composable
internal fun PlayingSongTitle(
    title: String,
    isCurrent: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val indicator = PLAYING_INDICATOR
    var offset by remember(indicator) { mutableIntStateOf(0) }

    LaunchedEffect(isCurrent, isPlaying, indicator) {
        offset = 0
        if (isCurrent && isPlaying && indicator.length > 1) {
            while (true) {
                delay(PLAYING_INDICATOR_FRAME_MILLIS)
                offset = (offset + 1) % indicator.length
            }
        }
    }

    val rotatedIndicator = indicator.drop(offset) + indicator.take(offset)
    Text(
        text = if (isCurrent) "$rotatedIndicator $title" else title,
        modifier =
            if (isCurrent) {
                modifier.clearAndSetSemantics {
                    contentDescription = if (isPlaying) "Playing $title" else "Paused $title"
                }
            } else {
                modifier
            },
        maxLines = 1
    )
}

private const val PLAYING_INDICATOR_FRAME_MILLIS = 200L
