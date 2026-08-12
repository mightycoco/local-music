package com.localmusic.player.data.stream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HttpStreamSourceResolverTest {
    @Test
    fun directHttpUrlCreatesOneStreamStation() = runTest {
        val stations = HttpStreamSourceResolver(ioDispatcher = Dispatchers.Unconfined).resolve(
            " https://radio.example.org/live.mp3 "
        )

        assertEquals(1, stations.size)
        assertEquals("radio.example.org", stations.single().title)
        assertEquals("https://radio.example.org/live.mp3", stations.single().uri)
    }

    @Test
    fun nonHttpUrlIsRejected() = runTest {
        try {
            HttpStreamSourceResolver(ioDispatcher = Dispatchers.Unconfined).resolve(
                "file:///music/station.mp3"
            )
            throw AssertionError("Expected invalid stream URL to be rejected")
        } catch (_: IllegalArgumentException) {
        }
    }
}