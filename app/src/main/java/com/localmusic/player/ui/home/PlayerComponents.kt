package com.localmusic.player.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localmusic.player.domain.model.Song

@Composable
internal fun MiniPlayer(
        song: Song,
        artworkUri: String?,
        isPlaying: Boolean,
        progress: Float,
        onOpenNowPlaying: () -> Unit,
        onPrevious: () -> Unit,
        onPlayPause: () -> Unit,
        onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ArtworkThumbnail(artworkUri = artworkUri)
            Column(
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenNowPlaying),
                    verticalArrangement = Arrangement.Center
            ) {
                Text(text = song.title, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                Text(text = song.album, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onPrevious) { Text(Glyphs.PLAYER_PREVIOUS.glyph) }
            IconButton(onClick = onPlayPause) {
                Text(if (isPlaying) Glyphs.PLAYER_PAUSE.glyph else Glyphs.PLAYER_PLAY.glyph)
            }
            IconButton(onClick = onNext) { Text(Glyphs.PLAYER_NEXT.glyph) }
        }
        LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(2.dp)
        )
    }
}
