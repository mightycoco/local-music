package com.localmusic.player.domain.repository

import com.localmusic.player.domain.model.Song

/** Boundary for local playback commands; UI never talks to Media3 directly. */
interface PlaybackController {
    fun play(songs: List<Song>, startSongId: String)
}