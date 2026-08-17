package com.localmusic.player.ui.home

import com.localmusic.player.domain.model.Song
import com.localmusic.player.playlist.M3uPlaylistEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistQueueEnqueueTest {
    @Test
    fun resolvesNewPlayableEntriesInPlaylistOrder() {
        val queuedSong = testSong("queued")
        val firstSong = testSong("first")
        val secondSong = testSong("second")

        val result =
            resolvePlaylistEntriesToEnqueue(
                queueEntries = listOf(queuedSong.toEntry()),
                playlistEntries = listOf(queuedSong.toEntry(), firstSong.toEntry(), secondSong.toEntry()),
                songsByUri = listOf(queuedSong, firstSong, secondSong).associateBy(Song::uri)
            )

        assertEquals(listOf(firstSong, secondSong), result)
    }

    @Test
    fun excludesRepeatedAndUnavailablePlaylistEntries() {
        val availableSong = testSong("available")
        val unavailableEntry = testEntry("unavailable")

        val result =
            resolvePlaylistEntriesToEnqueue(
                queueEntries = emptyList(),
                playlistEntries = listOf(availableSong.toEntry(), unavailableEntry, availableSong.toEntry()),
                songsByUri = mapOf(availableSong.uri to availableSong)
            )

        assertEquals(listOf(availableSong), result)
    }

    private fun testSong(id: String): Song =
        Song(
            id = id,
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            durationMillis = 180_000,
            dateAddedEpochSeconds = 1_700_000_000,
            folderName = "Music",
            uri = "content://media/$id"
        )

    private fun Song.toEntry(): M3uPlaylistEntry = testEntry(id)

    private fun testEntry(id: String): M3uPlaylistEntry =
        M3uPlaylistEntry(
            title = "Song $id",
            artist = "Artist",
            durationSeconds = 180,
            uri = "content://media/$id"
        )
    *** End Patch