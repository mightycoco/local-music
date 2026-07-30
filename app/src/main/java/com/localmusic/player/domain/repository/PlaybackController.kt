package com.localmusic.player.domain.repository

import com.localmusic.player.domain.model.Song
import kotlinx.coroutines.flow.Flow

/** Boundary for local playback commands; UI never talks to Media3 directly. */
interface PlaybackController {
    fun observePlayback(): Flow<PlaybackSnapshot>
    fun play(songs: List<Song>, startSongId: String)
    fun enqueue(song: Song)
    fun clearQueue()
    fun resume()
    fun pause()
    fun seekTo(progress: Float)
    fun setShuffleEnabled(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun setVisualizerEnabled(enabled: Boolean)
}

enum class RepeatMode {
    Off,
    One,
    All
}

data class PlaybackSnapshot(
        val songId: String? = null,
        val queueSongIds: List<String> = emptyList(),
        val isPlaying: Boolean = false,
        val positionMillis: Long = 0L,
        val durationMillis: Long = 0L,
        val isShuffleEnabled: Boolean = false,
        val repeatMode: RepeatMode = RepeatMode.Off,
        val visualizerLevels: List<Float> = emptyList()
) {
    val progress: Float
        get() =
                if (durationMillis > 0L) {
                    (positionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
}
