package com.localmusic.player.data.mediastore

/** Scans multiple local music sources before repository-level duplicate resolution. */
class CompositeMusicScanner(private val scanners: List<MusicScanner>) : MusicScanner {
    override suspend fun scan(): MusicScanResult {
        val results = scanners.map { scanner -> scanner.scan() }
        return MusicScanResult(
                songs = results.flatMap(MusicScanResult::songs),
                isComplete = results.all(MusicScanResult::isComplete)
        )
    }
}
