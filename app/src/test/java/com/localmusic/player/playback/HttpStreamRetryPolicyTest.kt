package com.localmusic.player.playback

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpStreamRetryPolicyTest {
    @Test
    fun httpStreamsRetryWithBoundedBackoff() {
        assertEquals(1_000L, HttpStreamRetryPolicy.retryDelayMillis(errorCount = 1))
        assertEquals(2_000L, HttpStreamRetryPolicy.retryDelayMillis(errorCount = 2))
        assertEquals(4_000L, HttpStreamRetryPolicy.retryDelayMillis(errorCount = 3))
        assertEquals(C.TIME_UNSET, HttpStreamRetryPolicy.retryDelayMillis(errorCount = 4))
    }

    @Test
    fun identifiesOnlyHttpAndHttpsUrisAsStreams() {
        assertTrue(HttpStreamRetryPolicy.isHttpStream("http://radio.example/live"))
        assertTrue(HttpStreamRetryPolicy.isHttpStream("https://radio.example/live"))
        assertFalse(HttpStreamRetryPolicy.isHttpStream("content://media/audio/1"))
        assertFalse(HttpStreamRetryPolicy.isHttpStream("file:///music/song.mp3"))
    }
}