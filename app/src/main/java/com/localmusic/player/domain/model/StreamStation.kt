package com.localmusic.player.domain.model

/** Metadata for an HTTP(S) stream resolved from a direct URL or an M3U source. */
data class StreamStation(
    val title: String,
    val artist: String,
    val durationSeconds: Long,
    val uri: String
)