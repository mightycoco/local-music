package com.localmusic.player.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class M3uPlaylistCodecTest {
    private val codec = M3uPlaylistCodec()

    @Test
    fun parseReadsExtendedM3uEntries() {
        val playlist = codec.parse(
            name = "Road",
            content = """
                #EXTM3U
                #EXTINF:181,The Artist - The Title
                content://music/song-1
            """.trimIndent()
        )

        assertEquals("Road", playlist.name)
        assertEquals(1, playlist.entries.size)
        assertEquals("The Artist", playlist.entries.first().artist)
        assertEquals("The Title", playlist.entries.first().title)
        assertEquals(181L, playlist.entries.first().durationSeconds)
        assertEquals("content://music/song-1", playlist.entries.first().uri)
    }

    @Test
    fun exportWritesExtendedM3uEntries() {
        val exported = codec.export(
            M3uPlaylist(
                name = "Road",
                entries = listOf(
                    M3uPlaylistEntry(
                        title = "The Title",
                        artist = "The Artist",
                        durationSeconds = 181,
                        uri = "content://music/song-1"
                    )
                )
            )
        )

        assertEquals(
            """
                #EXTM3U
                #EXTINF:181,The Artist - The Title
                content://music/song-1

            """.trimIndent(),
            exported
        )
    }
}