package com.localmusic.player.playlist

import com.localmusic.player.domain.model.Song

data class M3uPlaylist(
    val name: String,
    val entries: List<M3uPlaylistEntry>
) {
    companion object {
        const val QUEUE_NAME = "Queue"
        const val ONLINE_FAVOURITES_NAME = "__online_favourites__"
    }
}

fun List<M3uPlaylist>.withQueueFirst(): List<M3uPlaylist> = sortedWith(
    compareBy<M3uPlaylist> { it.name != M3uPlaylist.QUEUE_NAME }
        .thenBy { it.name.lowercase() }
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