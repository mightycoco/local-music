package com.localmusic.player.artwork

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkCachePrunerTest {
    private val pruner = ArtworkCachePruner()

    @Test
    fun entriesToDeleteReturnsOldestEntriesUntilUnderBudget() {
        val entries = listOf(
            ArtworkCacheEntry("new", sizeBytes = 10, lastModifiedEpochMillis = 3),
            ArtworkCacheEntry("old", sizeBytes = 10, lastModifiedEpochMillis = 1),
            ArtworkCacheEntry("middle", sizeBytes = 10, lastModifiedEpochMillis = 2)
        )

        val deleteKeys = pruner.entriesToDelete(entries, maxBytes = 15).map { it.key }

        assertEquals(listOf("old", "middle"), deleteKeys)
    }
}