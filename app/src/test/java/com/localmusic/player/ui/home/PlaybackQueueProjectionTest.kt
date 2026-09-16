package com.localmusic.player.ui.home

import com.localmusic.player.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueProjectionTest {
    @Test
    fun resolvesUniqueAvailableSongsInQueueOrder() {
        val first = testSong("song-1")
        val second = testSong("song-2")
        val third = testSong("song-3")

        val result =
            resolvePlaybackQueue(
                queueSongIds =
                    listOf(first.id, second.id, first.id, "missing-song", third.id),
                songsById = listOf(first, second, third).associateBy(Song::id)
            )

        assertEquals(listOf(first, second, third), result)
    }

    @Test
    fun ignoresIncoherentSnapshotsUntilRequestedSongAndQueueAreCurrent() {
        val pendingSongId = "song-outside-playlist"

        assertFalse(
            shouldApplyPlaybackSnapshot(pendingSongId, "playlist-song", listOf("playlist-song"))
        )
        assertFalse(shouldApplyPlaybackSnapshot(pendingSongId, null, emptyList()))
        assertFalse(shouldApplyPlaybackSnapshot(pendingSongId, pendingSongId, emptyList()))
        assertTrue(shouldApplyPlaybackSnapshot(pendingSongId, pendingSongId, listOf(pendingSongId)))
        assertTrue(shouldApplyPlaybackSnapshot(null, null, emptyList()))
    }

    @Test
    fun selectedSongBecomesReplacementQueueWhenPagingSnapshotIsEmpty() {
        val selectedSong = testSong("selected-local-mp3")

        assertEquals(listOf(selectedSong), resolveReplacementQueue(emptyList(), selectedSong))
    }

    @Test
    fun replacementQueueRetainsLoadedOrderWhenItContainsSelectedSong() {
        val first = testSong("first")
        val selectedSong = testSong("selected")

        assertEquals(
            listOf(first, selectedSong),
            resolveReplacementQueue(listOf(first, selectedSong, first), selectedSong)
        )
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
}
