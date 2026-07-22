package com.localmusic.player.playlist

import com.localmusic.player.domain.model.Song

data class M3uPlaylist(
    val name: String,
    val entries: List<M3uPlaylistEntry>
)

data class M3uPlaylistEntry(
    val title: String,
    val artist: String,
    val durationSeconds: Long,
    val uri: String
)

fun Song.toM3uEntry(): M3uPlaylistEntry = M3uPlaylistEntry(
    title = title,
    artist = artist,
    durationSeconds = durationMillis / 1_000L,
    uri = uri
)