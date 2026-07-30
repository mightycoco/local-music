package com.localmusic.player.ui.home

import com.localmusic.player.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQueueProjectionTest {
    @Test
    fun resolvesAvailableSongsInPlaybackOrder() {
        val first = testSong("song-1")
        val second = testSong("song-2")

        val result =
                resolvePlaybackQueue(
                        queueSongIds = listOf(second.id, "missing-song", first.id),
                        songsById = listOf(first, second).associateBy(Song::id)
                )

        assertEquals(listOf(second, first), result)
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
