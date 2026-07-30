package com.localmusic.player.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.repository.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingContent(
        uiState: HomeUiState,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onProgressChange: (Float) -> Unit,
        onShuffleToggle: () -> Unit,
        onRepeatCycle: () -> Unit,
        onVisualizerEnabledChange: (Boolean) -> Unit,
        onFavouriteToggle: (Song) -> Unit,
        onCreatePlaylist: (String) -> Unit,
        onAddToPlaylist: (String) -> Unit,
        onAddToQueue: () -> Unit,
        onShowQueue: () -> Unit,
        onReturnHome: () -> Unit
) {
    val song = uiState.nowPlayingSong
    val durationMillis =
            uiState.playbackDurationMillis.takeIf { it > 0L } ?: song?.durationMillis ?: 0L
    val elapsedMillis = (durationMillis * uiState.playbackProgress).toLong()
    val remainingMillis = (durationMillis - elapsedMillis).coerceAtLeast(0L)
    var showPlaylistChooser by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
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
        if (isWideLayout) {
            Row(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                        modifier = Modifier.weight(0.45f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NowPlayingHeader(
                            onReturnHome = onReturnHome,
                            onShowMoreActions = { showMoreActions = true }
                    )
                    ArtworkThumbnail(
                            artworkUri = uiState.artworkBySongId[song.id],
                            visualizerLevels = uiState.visualizerLevels,
                            isPlaying = uiState.isPlaying,
                            allowVisualizerToggle = true,
                            onVisualizerEnabledChange = onVisualizerEnabledChange,
                            modifier =
                                    Modifier.weight(1f)
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(MaterialTheme.shapes.large)
                    )
                }
                Column(
                        modifier = Modifier.weight(0.55f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NowPlayingDetailsAndControls(
                            uiState = uiState,
                            song = song,
                            elapsedMillis = elapsedMillis,
                            remainingMillis = remainingMillis,
                            onProgressChange = onProgressChange,
                            onPrevious = onPrevious,
                            onPlayPause = onPlayPause,
                            onNext = onNext
                    )
                    CurrentPlaylist(
                            songs = uiState.playbackQueue,
                            artworkBySongId = uiState.artworkBySongId,
                            nowPlayingSongId = song.id,
                            isPlaying = uiState.isPlaying,
                            modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                NowPlayingHeader(
                        onReturnHome = onReturnHome,
                        onShowMoreActions = { showMoreActions = true }
                )
                ArtworkThumbnail(
                        artworkUri = uiState.artworkBySongId[song.id],
                        visualizerLevels = uiState.visualizerLevels,
                        isPlaying = uiState.isPlaying,
                        allowVisualizerToggle = true,
                        onVisualizerEnabledChange = onVisualizerEnabledChange,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .weight(1f, fill = false)
                                        .aspectRatio(1f)
                                        .clip(MaterialTheme.shapes.large)
                )
                NowPlayingDetailsAndControls(
                        uiState = uiState,
                        song = song,
                        elapsedMillis = elapsedMillis,
                        remainingMillis = remainingMillis,
                        onProgressChange = onProgressChange,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext
                )
                CurrentPlaylist(
                        songs = uiState.playbackQueue,
                        artworkBySongId = uiState.artworkBySongId,
                        nowPlayingSongId = song.id,
                        isPlaying = uiState.isPlaying,
                        modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showMoreActions) {
        ModalBottomSheet(onDismissRequest = { showMoreActions = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                ListItem(
                        modifier = Modifier.clickable { onFavouriteToggle(song) },
                        headlineContent = {
                            Text(
                                    if (song.isFavourite) "Remove from favourites"
                                    else "Add to favourites"
                            )
                        },
                        leadingContent = {
                            Text(
                                    if (song.isFavourite) Glyphs.FAVOURITE_FULL.glyph
                                    else Glyphs.FAVOURITE.glyph,
                                    fontSize = 24.sp
                            )
                        }
                )
                ListItem(
                        modifier = Modifier.clickable { onShuffleToggle() },
                        headlineContent = { Text("Shuffle") },
                        supportingContent = { Text(if (uiState.isShuffleEnabled) "On" else "Off") },
                        leadingContent = {
                            Text(
                                    if (uiState.isShuffleEnabled) Glyphs.PLAYER_SHUFFLE.glyph
                                    else Glyphs.PLAYER_NOSHUFFLE.glyph,
                                    fontSize = 24.sp
                            )
                        }
                )
                ListItem(
                        modifier = Modifier.clickable { onRepeatCycle() },
                        headlineContent = { Text("Repeat") },
                        supportingContent = { Text(uiState.repeatMode.label) },
                        leadingContent = { Text(uiState.repeatMode.label, fontSize = 24.sp) }
                )
                ListItem(
                        modifier =
                                Modifier.clickable {
                                    showMoreActions = false
                                    showPlaylistChooser = true
                                },
                        headlineContent = { Text("Add to playlist") },
                        leadingContent = { Text("${Glyphs.PLAYLISTS.glyph}+", fontSize = 24.sp) }
                )
                ListItem(
                        modifier =
                                Modifier.clickable {
                                    onAddToQueue()
                                    showMoreActions = false
                                },
                        headlineContent = { Text("Add to queue") },
                        leadingContent = { Text("+${Glyphs.PLAYER_QUEUE.glyph}", fontSize = 24.sp) }
                )
                ListItem(
                        modifier =
                                Modifier.clickable {
                                    showMoreActions = false
                                    onShowQueue()
                                },
                        headlineContent = { Text("Show queue") },
                        leadingContent = { Text(Glyphs.PLAYER_QUEUE.glyph, fontSize = 24.sp) }
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
                            uiState.importedPlaylists.forEach { playlist ->
                                TextButton(
                                        onClick = {
                                            onAddToPlaylist(playlist.name)
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
                    onCreatePlaylist(name)
                    onAddToPlaylist(name.trim())
                    showCreatePlaylistDialog = false
                }
        )
    }
}

@Composable
private fun NowPlayingHeader(onReturnHome: () -> Unit, onShowMoreActions: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onReturnHome) { Text(Glyphs.HOME.glyph, fontSize = 26.sp) }
        Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onShowMoreActions) { Text(Glyphs.MORE.glyph, fontSize = 28.sp) }
    }
}

@Composable
private fun NowPlayingDetailsAndControls(
        uiState: HomeUiState,
        song: Song,
        elapsedMillis: Long,
        remainingMillis: Long,
        onProgressChange: (Float) -> Unit,
        onPrevious: () -> Unit,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
            )
            Text(
                    text =
                            listOf(song.artist, song.album)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • "),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
            )
        }
        Slider(
                value = uiState.playbackProgress.coerceIn(0f, 1f),
                onValueChange = onProgressChange,
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
                Text(Glyphs.PLAYER_PREVIOUS.glyph, fontSize = 36.sp)
            }
            Button(onClick = onPlayPause, modifier = Modifier.size(72.dp)) {
                Text(
                        text =
                                if (uiState.isPlaying) Glyphs.PLAYER_PAUSE.glyph
                                else Glyphs.PLAYER_PLAY.glyph,
                        fontSize = 34.sp
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
                Text(Glyphs.PLAYER_NEXT.glyph, fontSize = 36.sp)
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
        modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val currentSongIndex = songs.indexOfFirst { it.id == nowPlayingSongId }

    LaunchedEffect(nowPlayingSongId, songs) {
        if (currentSongIndex >= 0) listState.scrollToItem(currentSongIndex)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
                text = "Current playlist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
        )
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
                RepeatMode.Off -> "⇾"
                RepeatMode.One -> "⟲¹"
                RepeatMode.All -> "⟲"
            }
