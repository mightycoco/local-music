package com.localmusic.player.data.repository

import com.localmusic.player.domain.model.Song

/** Hides duplicate songs by URI, file signature, and normalized metadata signature. */
class DuplicateSongResolver {
    fun resolve(songs: List<Song>): List<Song> {
        val seenUris = mutableSetOf<String>()
        val seenFiles = mutableSetOf<String>()
        val seenMetadata = mutableSetOf<String>()

        return songs
            .sortedByDescending { it.dateAddedEpochSeconds }
            .filter { song ->
                val uriKey = song.uri.normalizedKey()
                val fileKey = listOf(song.fileName.normalizedKey(), song.durationMillis, song.sizeBytes).joinToString("|")
                val metadataKey = listOf(
                    song.title.normalizedKey(),
                    song.artist.normalizedKey(),
                    song.album.normalizedKey(),
                    song.durationMillis
                ).joinToString("|")

                val isDuplicate = uriKey in seenUris || fileKey in seenFiles || metadataKey in seenMetadata
                if (!isDuplicate) {
                    seenUris += uriKey
                    seenFiles += fileKey
                    seenMetadata += metadataKey
                }
                !isDuplicate
            }
    }

    private fun String.normalizedKey(): String = trim().lowercase()
}