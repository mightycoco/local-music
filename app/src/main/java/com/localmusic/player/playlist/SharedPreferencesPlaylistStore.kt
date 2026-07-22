package com.localmusic.player.playlist

import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.util.Base64

/** SharedPreferences-backed store for small user-managed M3U playlist lists. */
class SharedPreferencesPlaylistStore(
    private val sharedPreferences: SharedPreferences,
    private val codec: M3uPlaylistCodec = M3uPlaylistCodec()
) : PlaylistStore {
    override fun playlists(): List<M3uPlaylist> = sharedPreferences
        .getStringSet(KEY_PLAYLISTS, emptySet())
        .orEmpty()
        .mapNotNull { entry -> entry.decodePlaylist() }
        .sortedBy { it.name.lowercase() }

    override fun save(playlist: M3uPlaylist): List<M3uPlaylist> {
        val updatedPlaylists = (playlists().filterNot { it.name == playlist.name } + playlist)
            .sortedBy { it.name.lowercase() }
        sharedPreferences.edit()
            .putStringSet(KEY_PLAYLISTS, updatedPlaylists.map { it.encodePlaylist() }.toSet())
            .commit()
        return updatedPlaylists
    }

    override fun delete(name: String): List<M3uPlaylist> {
        val updatedPlaylists = playlists().filterNot { it.name == name }
        sharedPreferences.edit()
            .putStringSet(KEY_PLAYLISTS, updatedPlaylists.map { it.encodePlaylist() }.toSet())
            .commit()
        return updatedPlaylists
    }

    private fun M3uPlaylist.encodePlaylist(): String = listOf(name, codec.export(this))
        .joinToString(separator = FIELD_SEPARATOR) { value ->
            Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        }

    private fun String.decodePlaylist(): M3uPlaylist? {
        val parts = split(FIELD_SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        return runCatching {
            val name = String(Base64.getDecoder().decode(parts[0]), StandardCharsets.UTF_8)
            val content = String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8)
            codec.parse(name, content)
        }.getOrNull()
    }

    private companion object {
        const val KEY_PLAYLISTS = "playlists"
        const val FIELD_SEPARATOR = "\t"
    }
}