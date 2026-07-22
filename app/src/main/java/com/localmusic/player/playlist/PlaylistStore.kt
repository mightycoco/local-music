package com.localmusic.player.playlist

interface PlaylistStore {
    fun playlists(): List<M3uPlaylist>
    fun save(playlist: M3uPlaylist): List<M3uPlaylist>
    fun delete(name: String): List<M3uPlaylist>
}