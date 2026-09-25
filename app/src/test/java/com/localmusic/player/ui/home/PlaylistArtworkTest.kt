package com.localmusic.player.ui.home

import com.localmusic.player.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistArtworkTest {
    @Test
    fun `maps resolved playlist artwork by entry uri`() {
        val playlistSong =
            Song(
                id = "song-id",
                title = "Song",
                artist = "Artist",
                album = "Album",
                durationMillis = 180_000,
                dateAddedEpochSeconds = 0,
                folderName = "Music",
                uri = "content://music/song"
            )

        assertEquals(
            mapOf(playlistSong.uri to "file://artwork.jpg"),
            playlistArtworkByUri(
                songs = listOf(playlistSong),
                artworkBySongId = mapOf(playlistSong.id to "file://artwork.jpg")
            )
        )
    }
}