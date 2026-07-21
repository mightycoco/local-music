package com.localmusic.player.data.repository

import com.localmusic.player.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicateSongResolverTest {
    private val resolver = DuplicateSongResolver()

    @Test
    fun resolveKeepsNewestSongWhenMetadataMatches() {
        val older = song(id = "older", dateAdded = 10)
        val newer = song(id = "newer", dateAdded = 20)

        val result = resolver.resolve(listOf(older, newer))

        assertEquals(listOf(newer), result)
    }

    @Test
    fun resolveKeepsDistinctSongsSortedNewestFirst() {
        val oldest = song(id = "oldest", title = "First", dateAdded = 1)
        val newest = song(id = "newest", title = "Second", dateAdded = 3)
        val middle = song(id = "middle", title = "Third", dateAdded = 2)

        val result = resolver.resolve(listOf(oldest, newest, middle))

        assertEquals(listOf(newest, middle, oldest), result)
    }

    private fun song(
        id: String,
        title: String = "Song",
        dateAdded: Long
    ): Song = Song(
        id = id,
        fileName = "$title.flac",
        title = title,
        artist = "Artist",
        album = "Album",
        durationMillis = 180_000,
        dateAddedEpochSeconds = dateAdded,
        folderName = "Music",
        uri = "content://media/$id",
        mimeType = "audio/flac",
        sizeBytes = 1024
    )
}