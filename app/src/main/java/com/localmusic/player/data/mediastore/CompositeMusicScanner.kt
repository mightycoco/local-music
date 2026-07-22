package com.localmusic.player.data.mediastore

import com.localmusic.player.domain.model.Song

/** Scans multiple local music sources before repository-level duplicate resolution. */
class CompositeMusicScanner(private val scanners: List<MusicScanner>) : MusicScanner {
    override suspend fun scan(): List<Song> = scanners.flatMap { scanner -> scanner.scan() }
}