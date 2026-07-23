package com.localmusic.player.domain.model

object SmartPlaylistRules {
    private const val THIRTY_DAYS_MILLIS = 30L * 24L * 60L * 60L * 1_000L

    fun isNeverPlayed(song: Song): Boolean = song.playCount == 0

    fun wasPlayedWithinLastThirtyDays(song: Song, nowEpochMillis: Long): Boolean {
        val lastPlayed = song.lastPlayedEpochMillis ?: return false
        return lastPlayed in (nowEpochMillis - THIRTY_DAYS_MILLIS)..nowEpochMillis
    }
}