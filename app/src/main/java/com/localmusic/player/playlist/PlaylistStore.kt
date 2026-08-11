package com.localmusic.player.playlist

interface PlaylistStore {
    suspend fun playlists(): List<M3uPlaylist>
    suspend fun save(playlist: M3uPlaylist)
    suspend fun delete(name: String)
}
