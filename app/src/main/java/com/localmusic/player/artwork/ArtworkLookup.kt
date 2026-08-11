package com.localmusic.player.artwork

import android.net.Uri
import com.localmusic.player.domain.model.Song
import java.security.MessageDigest

/** Resolves stable artwork cache keys and candidate artwork URIs for local songs. */
class ArtworkLookup {
    fun cacheKey(song: Song): String =
            listOf(song.artist, song.album)
                    .joinToString("") { value ->
                        val normalized = value.trim().lowercase()
                        "${normalized.length}:$normalized"
                    }
                    .sha256()

    fun embeddedArtworkCandidate(song: Song): Uri = Uri.parse(song.uri)

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
