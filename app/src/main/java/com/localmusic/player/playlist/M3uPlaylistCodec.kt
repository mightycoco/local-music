package com.localmusic.player.playlist

/** Imports and exports extended M3U playlists using local content/file URIs. */
class M3uPlaylistCodec {
    fun parse(name: String, content: String): M3uPlaylist {
        val entries = mutableListOf<M3uPlaylistEntry>()
        var pendingInfo: M3uPlaylistEntry? = null

        content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            when {
                line.startsWith("#EXTINF:") -> pendingInfo = line.parseExtInf()
                line.startsWith("#") -> Unit
                else -> {
                    val info = pendingInfo
                    entries +=
                            if (info == null) {
                                M3uPlaylistEntry(
                                        title = line.substringAfterLast('/'),
                                        artist = "Unknown Artist",
                                        durationSeconds = -1L,
                                        uri = line
                                )
                            } else {
                                info.copy(uri = line)
                            }
                    pendingInfo = null
                }
            }
        }

        return M3uPlaylist(name = name, entries = entries)
    }

    fun export(playlist: M3uPlaylist): String = buildString { write(playlist, this) }

    fun write(playlist: M3uPlaylist, output: Appendable) {
        output.appendLine("#EXTM3U")
        playlist.entries.forEach { entry ->
            output.appendLine("#EXTINF:${entry.durationSeconds},${entry.artist} - ${entry.title}")
            output.appendLine(entry.uri)
        }
    }

    private fun String.parseExtInf(): M3uPlaylistEntry {
        val payload = removePrefix("#EXTINF:")
        val duration = payload.substringBefore(',', "-1").toLongOrNull() ?: -1L
        val label = payload.substringAfter(',', "")
        val artist = label.substringBefore(" - ", "Unknown Artist").ifBlank { "Unknown Artist" }
        val title = label.substringAfter(" - ", label).ifBlank { "Unknown Title" }
        return M3uPlaylistEntry(
                title = title,
                artist = artist,
                durationSeconds = duration,
                uri = ""
        )
    }
}
