package com.localmusic.player.artwork

/** Selects the oldest artwork cache entries to delete when the cache exceeds its byte budget. */
class ArtworkCachePruner {
    fun entriesToDelete(entries: List<ArtworkCacheEntry>, maxBytes: Long): List<ArtworkCacheEntry> {
        if (maxBytes <= 0L) return entries.sortedBy { it.lastModifiedEpochMillis }

        var retainedBytes = entries.sumOf { it.sizeBytes }
        return entries
            .sortedBy { it.lastModifiedEpochMillis }
            .takeWhile { entry ->
                val shouldDelete = retainedBytes > maxBytes
                if (shouldDelete) retainedBytes -= entry.sizeBytes
                shouldDelete
            }
    }
}