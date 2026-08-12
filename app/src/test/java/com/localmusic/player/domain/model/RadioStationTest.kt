package com.localmusic.player.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RadioStationTest {
    @Test
    fun convertsDirectoryStationToPersistableStreamStation() {
        val station =
            RadioStation(
                id = "station-1",
                name = "Example FM",
                streamUrl = "https://radio.example.org/live",
                faviconUrl = "https://radio.example.org/icon.png"
            )

        val streamStation = station.toStreamStation()

        assertEquals("Example FM", streamStation.title)
        assertEquals("Online radio", streamStation.artist)
        assertEquals(-1L, streamStation.durationSeconds)
        assertEquals("https://radio.example.org/live", streamStation.uri)
        assertEquals("https://radio.example.org/icon.png", streamStation.artworkUri)
    }
}