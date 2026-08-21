package com.localmusic.player.ui.home

import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class LibraryProjectionPerformanceTest {
    @Test
    fun `projects fifty thousand songs within the responsiveness budget`() {
        val songs = (0 until SONG_COUNT).map(::testSong)
        val projection =
            LibraryProjection(
                filter = LibraryFilter.Favourites,
                browseValue = null,
                sortOrder = SortOrder.Name,
                searchQuery = "artist 4",
                playlists = emptyList()
            )

        lateinit var result: List<Song>
        val elapsedMillis = measureTimeMillis {
            result = projectLibrarySongs(songs, projection)
        }

        assertEquals(612, result.size)
        assertTrue(
            "Filtering and sorting $SONG_COUNT songs took ${elapsedMillis}ms; budget is ${BUDGET_MILLIS}ms",
            elapsedMillis <= BUDGET_MILLIS
        )
    }

    private fun testSong(index: Int): Song =
        Song(
            id = "song-$index",
            title = "Track ${SONG_COUNT - index}",
            artist = "Artist ${index % 100}",
            album = "Album ${index % 500}",
            durationMillis = 180_000L + index,
            dateAddedEpochSeconds = 1_700_000_000L + index,
            folderName = "Music ${index % 20}",
            uri = "content://media/$index",
            playCount = if (index % 9 == 4) 1 else 0,
            isFavourite = index % 9 == 4
        )

    private companion object {
        const val SONG_COUNT = 50_000
        const val BUDGET_MILLIS = 1_500L
    }
}