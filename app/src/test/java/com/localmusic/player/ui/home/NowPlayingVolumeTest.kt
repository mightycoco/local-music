package com.localmusic.player.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingVolumeTest {
    @Test
    fun `upward drag increases volume proportionally`() {
        assertEquals(
            10,
            volumeForVerticalDrag(
                startingVolume = 5,
                maximumVolume = 15,
                dragDistance = -100f,
                dragRange = 300
            )
        )
    }

    @Test
    fun `downward drag decreases volume proportionally`() {
        assertEquals(
            5,
            volumeForVerticalDrag(
                startingVolume = 10,
                maximumVolume = 15,
                dragDistance = 100f,
                dragRange = 300
            )
        )
    }

    @Test
    fun `volume is clamped to valid stream range`() {
        assertEquals(15, volumeForVerticalDrag(14, 15, -300f, 300))
        assertEquals(0, volumeForVerticalDrag(1, 15, 300f, 300))
    }
}