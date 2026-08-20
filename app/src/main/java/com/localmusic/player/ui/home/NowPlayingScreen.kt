package com.localmusic.player.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SongSource
import com.localmusic.player.playlist.M3uPlaylist
import com.localmusic.player.domain.repository.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingContent(
    uiState: HomeUiState,
    playbackActions: PlaybackActions,
    songActions: SongActions,
    playlistActions: PlaylistFeatureActions,
    onShowQueue: () -> Unit
) {
    val song = uiState.nowPlayingSong
    val durationMillis =
        uiState.playbackDurationMillis.takeIf { it > 0L } ?: song?.durationMillis ?: 0L
    val elapsedMillis = (durationMillis * uiState.playbackProgress).toLong()
    val remainingMillis = (durationMillis - elapsedMillis).coerceAtLeast(0L)
    var showPlaylistChooser by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var isPreviousCoverTransition by remember { mutableStateOf(false) }
    val playlistState = rememberLazyListState()
    val isPlaylistDragged by playlistState.interactionSource.collectIsDraggedAsState()
    val isPlaylistAtTop by remember {
        derivedStateOf {
            playlistState.firstVisibleItemIndex == 0 &&
                    playlistState.firstVisibleItemScrollOffset == 0
        }
    }
    var hasUserScrolledPlaylist by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaylistAtTop, isPlaylistDragged) {
        when {
            isPlaylistAtTop -> hasUserScrolledPlaylist = false
            isPlaylistDragged -> hasUserScrolledPlaylist = true
        }
    }
    if (song == null) {
        EmptyState(
            title = "Nothing playing",
            message = "Choose a song from Home or Favourites to start playback."
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        ArtworkBackdrop(artworkUri = uiState.artworkBySongId[song.id])
        val isWideLayout = maxWidth > maxHeight
        val portraitArtworkMaxHeight = maxHeight * 0.42f
        val portraitArtworkHeight by animateDpAsState(
            targetValue =
                if (hasUserScrolledPlaylist) portraitArtworkMaxHeight / 2 else portraitArtworkMaxHeight,
            label = "portraitArtworkHeight"
        )
        val layoutState =
            NowPlayingLayoutState(
                song = song,
                artworkUri = uiState.artworkBySongId[song.id],
                artworkBySongId = uiState.artworkBySongId,
                visualizerLevels = uiState.visualizerLevels,
                playbackQueue = uiState.playbackQueue,
                isPlaying = uiState.isPlaying,
                playbackProgress = uiState.playbackProgress,
                isVisualizerPreferred = uiState.isVisualizerPreferred,
                elapsedMillis = elapsedMillis,
                remainingMillis = remainingMillis,
                isPreviousCoverTransition = isPreviousCoverTransition
            )
        val layoutActions =
            NowPlayingLayoutActions(
                playback = playbackActions,
                selectQueueSong = songActions.playQueued,
                toggleFavourite = songActions.toggleFavourite,
                showMoreActions = { showMoreActions = true },
                setPreviousCoverTransition = { isPreviousCoverTransition = it }
            )
        if (isWideLayout) {
            LandscapeNowPlayingLayout(
                state = layoutState,
                playlistState = playlistState,
                actions = layoutActions
            )
        } else {
            PortraitNowPlayingLayout(
                state = layoutState,
                artworkHeight = portraitArtworkHeight,
                playlistState = playlistState,
                actions = layoutActions
            )
        }
    }

    if (showMoreActions) {
        ModalBottomSheet(onDismissRequest = { showMoreActions = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                ListItem(
                    modifier = Modifier.clickable { songActions.toggleFavourite(song) },
                    headlineContent = {
                        Text(
                            if (song.isFavourite) "Remove from favourites"
                            else "Add to favourites"
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector =
                                if (song.isFavourite) Icons.Filled.Favorite
                                else Icons.Outlined.FavoriteBorder,
                            contentDescription = null
                        )
                    }
                )
                ListItem(
                    modifier = Modifier.clickable(onClick = playbackActions.toggleShuffle),
                    headlineContent = { Text("Shuffle") },
                    supportingContent = { Text(if (uiState.isShuffleEnabled) "On" else "Off") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Shuffle,
                            contentDescription = null
                        )
                    }
                )
                ListItem(
                    modifier = Modifier.clickable(onClick = playbackActions.cycleRepeatMode),
                    headlineContent = { Text("Repeat") },
                    supportingContent = { Text(uiState.repeatMode.label) },
                    leadingContent = {
                        Icon(
                            imageVector =
                                if (uiState.repeatMode == RepeatMode.One) {
                                    Icons.Outlined.RepeatOne
                                } else {
                                    Icons.Outlined.Repeat
                                },
                            contentDescription = null
                        )
                    }
                )
                ListItem(
                    modifier =
                        Modifier.clickable {
                            showMoreActions = false
                            showPlaylistChooser = true
                        },
                    headlineContent = { Text("Add to playlist") },
                    leadingContent = {
                        Icon(imageVector = Icons.Outlined.PlaylistAdd, contentDescription = null)
                    }
                )
                ListItem(
                    modifier =
                        Modifier.clickable {
                            playlistActions.nowPlaying.addToQueue()
                            showMoreActions = false
                        },
                    headlineContent = { Text("Add to queue") },
                    leadingContent = {
                        Icon(imageVector = Icons.Outlined.QueueMusic, contentDescription = null)
                    }
                )
                ListItem(
                    modifier =
                        Modifier.clickable {
                            showMoreActions = false
                            onShowQueue()
                        },
                    headlineContent = { Text("Show queue") },
                    leadingContent = {
                        Icon(imageVector = Icons.Outlined.QueueMusic, contentDescription = null)
                    }
                )
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
                        uiState.importedPlaylists
                            .filterNot {
                                it.name == M3uPlaylist.ONLINE_FAVOURITES_NAME
                            }
                            .forEach { playlist ->
                                TextButton(
                                    onClick = {
                                        playlistActions.nowPlaying.addToPlaylist(playlist.name)
                                        showPlaylistChooser = false
                                    }
                                ) { Text(playlist.name) }
                            }
                    }
                    Button(
                        onClick = {
                            showPlaylistChooser = false
                            showCreatePlaylistDialog = true
                        }
                    ) { Text("New playlist") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistChooser = false }) { Text("Cancel") }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                playlistActions.catalog.create(name)
                playlistActions.nowPlaying.addToPlaylist(name.trim())
                showCreatePlaylistDialog = false
            }
        )
    }
}

@Immutable
private data class NowPlayingLayoutState(
    val song: Song,
    val artworkUri: String?,
    val artworkBySongId: Map<String, String>,
    val visualizerLevels: List<Float>,
    val playbackQueue: List<Song>,
    val isPlaying: Boolean,
    val playbackProgress: Float,
    val isVisualizerPreferred: Boolean,
    val elapsedMillis: Long,
    val remainingMillis: Long,
    val isPreviousCoverTransition: Boolean
)

@Immutable
private data class NowPlayingLayoutActions(
    val playback: PlaybackActions,
    val selectQueueSong: (Song) -> Unit,
    val toggleFavourite: (Song) -> Unit,
    val showMoreActions: () -> Unit,
    val setPreviousCoverTransition: (Boolean) -> Unit
)

@Composable
private fun LandscapeNowPlayingLayout(
    state: NowPlayingLayoutState,
    playlistState: androidx.compose.foundation.lazy.LazyListState,
    actions: NowPlayingLayoutActions
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier =
                Modifier.weight(0.45f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.large)
        ) {
            NowPlayingArtwork(
                state = state,
                actions = actions,
                modifier = Modifier.fillMaxSize()
            )
            NowPlayingPlaybackControls(
                state = state,
                actions = actions,
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        CurrentPlaylist(
            songs = state.playbackQueue,
            artworkBySongId = state.artworkBySongId,
            nowPlayingSongId = state.song.id,
            isPlaying = state.isPlaying,
            onSongSelected = actions.selectQueueSong,
            onFavouriteToggle = actions.toggleFavourite,
            listState = playlistState,
            modifier = Modifier.weight(0.55f).fillMaxHeight()
        )
    }
}

@Composable
private fun PortraitNowPlayingLayout(
    state: NowPlayingLayoutState,
    artworkHeight: androidx.compose.ui.unit.Dp,
    playlistState: androidx.compose.foundation.lazy.LazyListState,
    actions: NowPlayingLayoutActions
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        NowPlayingArtwork(
            state = state,
            actions = actions,
            modifier =
                Modifier.fillMaxWidth()
                    .height(artworkHeight)
                    .clip(MaterialTheme.shapes.large)
        )
        NowPlayingPlaybackControls(
            state = state,
            actions = actions
        )
        CurrentPlaylist(
            songs = state.playbackQueue,
            artworkBySongId = state.artworkBySongId,
            nowPlayingSongId = state.song.id,
            isPlaying = state.isPlaying,
            onSongSelected = actions.selectQueueSong,
            onFavouriteToggle = actions.toggleFavourite,
            listState = playlistState,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NowPlayingArtwork(
    state: NowPlayingLayoutState,
    actions: NowPlayingLayoutActions,
    modifier: Modifier = Modifier
) {
    AnimatedNowPlayingArtwork(
        song = state.song,
        artworkUri = state.artworkUri,
        visualizerLevels = state.visualizerLevels,
        isPlaying = state.isPlaying,
        preferVisualizer = state.isVisualizerPreferred,
        isPreviousTransition = state.isPreviousCoverTransition,
        onPrevious = {
            actions.setPreviousCoverTransition(true)
            actions.playback.previous()
        },
        onNext = {
            actions.setPreviousCoverTransition(false)
            actions.playback.next()
        },
        onFavouriteToggle = { actions.toggleFavourite(state.song) },
        onShowMoreActions = actions.showMoreActions,
        onVisualizerEnabledChange = actions.playback.setVisualizerEnabled,
        modifier = modifier
    )
}

@Composable
private fun NowPlayingPlaybackControls(
    state: NowPlayingLayoutState,
    actions: NowPlayingLayoutActions,
    modifier: Modifier = Modifier
) {
    PlaybackControls(
        playbackProgress = state.playbackProgress,
        isPlaying = state.isPlaying,
        isSeekEnabled = state.song.source != SongSource.STREAM,
        elapsedMillis = state.elapsedMillis,
        remainingMillis = state.remainingMillis,
        onProgressChange = actions.playback.seekTo,
        onPrevious = {
            actions.setPreviousCoverTransition(true)
            actions.playback.previous()
        },
        onPlayPause = actions.playback.playPause,
        onNext = {
            actions.setPreviousCoverTransition(false)
            actions.playback.next()
        },
        modifier = modifier
    )
}

@Composable
private fun PlaybackControls(
    playbackProgress: Float,
    isPlaying: Boolean,
    isSeekEnabled: Boolean,
    elapsedMillis: Long,
    remainingMillis: Long,
    onProgressChange: (Float) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingProgress by remember { mutableFloatStateOf(playbackProgress) }
    LaunchedEffect(playbackProgress) { pendingProgress = playbackProgress }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Slider(
            value = pendingProgress.coerceIn(0f, 1f),
            onValueChange = { pendingProgress = it },
            onValueChangeFinished = { onProgressChange(pendingProgress) },
            enabled = isSeekEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatPlaybackTime(elapsedMillis), style = MaterialTheme.typography.bodySmall)
            Text(
                "-${formatPlaybackTime(remainingMillis)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(64.dp)) {
                Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
            Button(onClick = onPlayPause, modifier = Modifier.size(72.dp)) {
                Icon(
                    imageVector =
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next")
            }
        }
    }
}

@Composable
private fun CurrentPlaylist(
    songs: List<Song>,
    artworkBySongId: Map<String, String>,
    nowPlayingSongId: String,
    isPlaying: Boolean,
    onSongSelected: (Song) -> Unit,
    onFavouriteToggle: (Song) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(nowPlayingSongId, songs) {
        val currentSongIndex = songs.indexOfFirst { it.id == nowPlayingSongId }
        if (currentSongIndex >= 0) listState.scrollToItem(currentSongIndex)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (songs.isEmpty()) {
            Text("The active playlist is empty.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(items = songs, key = { it.id }) { queueSong ->
                    ListItem(
                        modifier = Modifier.clickable { onSongSelected(queueSong) },
                        leadingContent = {
                            ArtworkThumbnail(
                                artworkUri = artworkBySongId[queueSong.id],
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        headlineContent = {
                            PlayingSongTitle(
                                title = queueSong.title,
                                isCurrent = queueSong.id == nowPlayingSongId,
                                isPlaying = isPlaying
                            )
                        },
                        supportingContent = {
                            Text(
                                text = queueSong.artist,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onFavouriteToggle(queueSong) }) {
                                Icon(
                                    imageVector =
                                        if (queueSong.isFavourite) Icons.Filled.Favorite
                                        else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun formatPlaybackTime(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1_000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private val RepeatMode.label: String
    get() =
        when (this) {
            RepeatMode.Off -> "Off"
            RepeatMode.One -> "One"
            RepeatMode.All -> "All"
        }
