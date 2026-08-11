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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val carModeModifier = if (isCarMode) 2 else 1
    val artworkThumbnailSize = 48.dp * carModeModifier
    val controlSize = 48.dp * carModeModifier
    val playerPadding = 8.dp * carModeModifier
    val controlPadding = 4.dp * carModeModifier

    Column(
            modifier = Modifier.fillMaxWidth().padding(playerPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(controlPadding)
        ) {
            if (!isCarMode) {
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
                    Text(text = song.title, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                    Text(text = song.album, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                        modifier = Modifier.padding(horizontal = controlPadding),
                        horizontalArrangement = Arrangement.spacedBy(controlPadding)
                ) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(controlSize)) {
                        Text(Glyphs.PLAYER_PREVIOUS.glyph)
                    }
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(controlSize)) {
                        Text(if (isPlaying) Glyphs.PLAYER_PAUSE.glyph else Glyphs.PLAYER_PLAY.glyph)
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(controlSize)) {
                        Text(Glyphs.PLAYER_NEXT.glyph)
                    }
                }
            } else {
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
                    Text( text = song.title, maxLines = 2, style = MaterialTheme.typography.titleLarge)
                    Text(text = song.album, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                    Row(
                        modifier = Modifier.padding(horizontal = controlPadding),
                        horizontalArrangement = Arrangement.spacedBy(controlPadding)
                    ) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(controlSize)) {
                            Text(Glyphs.PLAYER_PREVIOUS.glyph)
                        }
                        IconButton(onClick = onPlayPause, modifier = Modifier.size(controlSize)) {
                            Text(if (isPlaying) Glyphs.PLAYER_PAUSE.glyph else Glyphs.PLAYER_PLAY.glyph)
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(controlSize)) {
                            Text(Glyphs.PLAYER_NEXT.glyph)
                        }
                    }
                }
            }
        }
        Slider(
                value = pendingProgress.coerceIn(0f, 1f),
                onValueChange = { pendingProgress = it },
                onValueChangeFinished = { onProgressChange(pendingProgress) },
                modifier = Modifier.fillMaxWidth().padding(bottom = controlPadding)
        )
    }
}
