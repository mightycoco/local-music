package com.localmusic.player.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartPlaylistRulesTest {
    @Test
    fun neverPlayedIncludesOnlySongsWithNoCompletedPlays() {
        assertTrue(SmartPlaylistRules.isNeverPlayed(song(playCount = 0)))
        assertFalse(SmartPlaylistRules.isNeverPlayed(song(playCount = 1)))
    }

    @Test
    fun lastThirtyDaysIncludesOnlyRecentPastPlays() {
        val now = 2_000_000_000_000L

        assertTrue(SmartPlaylistRules.wasPlayedWithinLastThirtyDays(song(lastPlayed = now), now))
        assertTrue(SmartPlaylistRules.wasPlayedWithinLastThirtyDays(song(lastPlayed = now - 30L * DAY), now))
        assertFalse(SmartPlaylistRules.wasPlayedWithinLastThirtyDays(song(lastPlayed = now - 30L * DAY - 1L), now))
        assertFalse(SmartPlaylistRules.wasPlayedWithinLastThirtyDays(song(lastPlayed = now + 1L), now))
        assertFalse(SmartPlaylistRules.wasPlayedWithinLastThirtyDays(song(lastPlayed = null), now))
    }

    private fun song(playCount: Int = 0, lastPlayed: Long? = null) = Song(
        id = "song",
        title = "Song",
        artist = "Artist",
        album = "Album",
        durationMillis = 1_000L,
        dateAddedEpochSeconds = 1L,
        folderName = "Music",
        uri = "content://media/song",
        playCount = playCount,
        lastPlayedEpochMillis = lastPlayed
    )

    private companion object {
        const val DAY = 24L * 60L * 60L * 1_000L
    }
}