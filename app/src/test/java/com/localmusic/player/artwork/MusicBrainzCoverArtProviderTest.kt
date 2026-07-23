package com.localmusic.player.artwork

import com.localmusic.player.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicBrainzCoverArtProviderTest {
    @Test
    fun artworkForUsesExactAlbumReleaseBeforeArtistFallback() {
        val transport = FakeTransport(
            responses = mapOf(
                "release%3A%22Album%22+AND+artist%3A%22Artist%22" to """{"releases":[{"id":"album-release"}]}""".toByteArray(),
                "coverartarchive.org/release/album-release/front-250" to byteArrayOf(1, 2, 3)
            )
        )

        val artwork = MusicBrainzCoverArtProvider(transport).artworkFor(song())

        assertEquals(listOf(
            "https://musicbrainz.org/ws/2/release?fmt=json&limit=1&query=release%3A%22Album%22+AND+artist%3A%22Artist%22",
            "https://coverartarchive.org/release/album-release/front-250"
        ), transport.requestedUrls)
        assertEquals(byteArrayOf(1, 2, 3).toList(), artwork?.toList())
    }

    @Test
    fun artworkForFallsBackToArtistReleaseWhenExactCoverIsUnavailable() {
        val transport = FakeTransport(
            responses = mapOf(
                "release%3A%22Album%22+AND+artist%3A%22Artist%22" to """{"releases":[{"id":"album-release"}]}""".toByteArray(),
                "artist%3A%22Artist%22+AND+primarytype%3Aalbum" to """{"releases":[{"id":"artist-release"}]}""".toByteArray(),
                "coverartarchive.org/release/artist-release/front-250" to byteArrayOf(4, 5, 6)
            )
        )

        val artwork = MusicBrainzCoverArtProvider(transport).artworkFor(song())

        assertEquals("https://coverartarchive.org/release/artist-release/front-250", transport.requestedUrls.last())
        assertEquals(byteArrayOf(4, 5, 6).toList(), artwork?.toList())
    }

    @Test
    fun artworkForSkipsUnknownMetadata() {
        val transport = FakeTransport(emptyMap())

        val artwork = MusicBrainzCoverArtProvider(transport).artworkFor(song(artist = "Unknown Artist"))

        assertNull(artwork)
        assertEquals(emptyList<String>(), transport.requestedUrls)
    }

    @Test
    fun rateLimiterWaitsBetweenRequests() {
        val waits = mutableListOf<Long>()
        val limiter = ArtworkRequestRateLimiter(
            minimumIntervalMillis = 1_100L,
            clock = { 0L },
            sleep = waits::add
        )

        limiter.awaitTurn()
        limiter.awaitTurn()

        assertEquals(listOf(1_100L), waits)
    }

    private fun song(artist: String = "Artist") = Song(
        id = "song",
        title = "Title",
        artist = artist,
        album = "Album",
        durationMillis = 1L,
        dateAddedEpochSeconds = 1L,
        folderName = "Music",
        uri = "content://media/song"
    )

    private class FakeTransport(
        private val responses: Map<String, ByteArray>
    ) : ArtworkHttpTransport {
        val requestedUrls = mutableListOf<String>()

        override fun get(url: String): ByteArray? {
            requestedUrls += url
            return responses.entries.firstOrNull { (fragment, _) -> url.contains(fragment) }?.value
        }
    }
}