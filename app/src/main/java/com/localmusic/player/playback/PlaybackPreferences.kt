package com.localmusic.player.playback

import android.content.SharedPreferences

/** Persists the last selected item from the durable playback queue. */
class PlaybackPreferences(private val sharedPreferences: SharedPreferences) {
    fun lastPlayedSongUri(): String? = sharedPreferences.getString(KEY_LAST_PLAYED_SONG_URI, null)

    fun setLastPlayedSongUri(uri: String?) {
        sharedPreferences.edit().apply {
            if (uri == null) remove(KEY_LAST_PLAYED_SONG_URI)
            else putString(KEY_LAST_PLAYED_SONG_URI, uri)
        }.apply()
    }

    private companion object {
        const val KEY_LAST_PLAYED_SONG_URI = "last_played_song_uri"
    }
}