package com.localmusic.player.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localmusic.player.domain.model.Song

@Composable
internal fun MiniPlayer(
        song: Song,
        artworkUri: String?,
        isPlaying: Boolean,
        progress: Float,
        isCarMode: Boolean,
        onOpenNowPlaying: () -> Unit,
        onPrevious: () -> Unit,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onProgressChange: (Float) -> Unit
) {
    var pendingProgress by remember { mutableFloatStateOf(progress) }
    LaunchedEffect(progress) { pendingProgress = progress }
    val artworkThumbnailSize = if (isCarMode) 88.dp else 64.dp
    val controlSize = if (isCarMode) 72.dp else 56.dp
    val playerPadding = if (isCarMode) 16.dp else 10.dp
    val contentSpacing = if (isCarMode) 16.dp else 8.dp

    Column(
            modifier = Modifier.fillMaxWidth().padding(playerPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(contentSpacing),
                verticalAlignment = Alignment.CenterVertically
        ) {
            ArtworkThumbnail(
                    artworkUri = artworkUri,
                    modifier =
                            Modifier.size(artworkThumbnailSize)
                                    .clickable(onClick = onOpenNowPlaying)
            )
            Column(
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenNowPlaying),
                    verticalArrangement = Arrangement.Center
            ) {
                Text(
                        text = song.title,
                        maxLines = if (isCarMode) 2 else 1,
                        style =
                                if (isCarMode) MaterialTheme.typography.titleLarge
                                else MaterialTheme.typography.titleMedium
                )
                Text(
                        text = song.album,
                        maxLines = 1,
                        style =
                                if (isCarMode) MaterialTheme.typography.bodyLarge
                                else MaterialTheme.typography.bodyMedium
                )
            }
            if (!isCarMode) {
                MiniPlayerControls(
                        isPlaying = isPlaying,
                        controlSize = controlSize,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext
                )
            }
        }
        if (isCarMode) {
            MiniPlayerControls(
                    isPlaying = isPlaying,
                    controlSize = controlSize,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext
            )
        }
        Slider(
                value = pendingProgress.coerceIn(0f, 1f),
                onValueChange = { pendingProgress = it },
                onValueChangeFinished = { onProgressChange(pendingProgress) },
                modifier = Modifier.fillMaxWidth().padding(bottom = contentSpacing)
        )
    }
}

@Composable
private fun MiniPlayerControls(
        isPlaying: Boolean,
        controlSize: androidx.compose.ui.unit.Dp,
        modifier: Modifier = Modifier,
        horizontalArrangement: Arrangement.Horizontal,
        onPrevious: () -> Unit,
        onPlayPause: () -> Unit,
        onNext: () -> Unit
) {
    Row(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(controlSize)) {
            Text(Glyphs.PLAYER_PREVIOUS.glyph, fontSize = 24.sp)
        }
        IconButton(onClick = onPlayPause, modifier = Modifier.size(controlSize)) {
            Text(
                    if (isPlaying) Glyphs.PLAYER_PAUSE.glyph else Glyphs.PLAYER_PLAY.glyph,
                    fontSize = 28.sp
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(controlSize)) {
            Text(Glyphs.PLAYER_NEXT.glyph, fontSize = 24.sp)
        }
    }
}
