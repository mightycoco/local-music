package com.localmusic.player.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBrowserTest {
    @Test
    fun artistValuesAreDistinctAndCaseInsensitiveSorted() {
        val songs = listOf(song("Zebra"), song("alpha"), song("Zebra"))

        assertEquals(listOf("alpha", "Zebra"), LibraryBrowser.values(songs, LibraryFilter.Artists))
    }

    @Test
    fun selectedFolderMatchesOnlySongsInThatFolder() {
        val downloadsSong = song(folder = "Downloads")
        val musicSong = song(folder = "Music")

        assertTrue(LibraryBrowser.matches(downloadsSong, LibraryFilter.Folders, "downloads"))
        assertFalse(LibraryBrowser.matches(musicSong, LibraryFilter.Folders, "downloads"))
    }

    @Test
    fun genreValuesAreAvailableForBrowsing() {
        val songs = listOf(song(genre = "Rock"), song(genre = "Jazz"), song(genre = "Rock"))

        assertEquals(listOf("Jazz", "Rock"), LibraryBrowser.values(songs, LibraryFilter.Genres))
    }

    private fun song(
        artist: String = "Artist",
        folder: String = "Music",
        genre: String = "Unknown Genre"
    ) = Song(
        id = "$artist-$folder",
        title = "Song",
        artist = artist,
        album = "Album",
        genre = genre,
        durationMillis = 1_000L,
        dateAddedEpochSeconds = 1L,
        folderName = folder,
        uri = "content://media/$artist-$folder"
    )
}