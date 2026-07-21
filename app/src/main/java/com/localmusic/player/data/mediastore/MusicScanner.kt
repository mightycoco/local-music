package com.localmusic.player.data.mediastore

import com.localmusic.player.domain.model.Song

/** Source abstraction for local music scans such as MediaStore and SAF folders. */
interface MusicScanner {
    suspend fun scan(): List<Song>
}