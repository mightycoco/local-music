package com.localmusic.player.artwork

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkDiskCacheTest {
    @Test
    fun clearDeletesCachedArtworkAndResetsSize() {
        val cacheRoot = Files.createTempDirectory("artwork-cache-test").toFile()
        try {
            val artworkDirectory = File(cacheRoot, "artwork").apply { mkdirs() }
            File(artworkDirectory, "first.art").writeBytes(ByteArray(12))
            File(artworkDirectory, "second.art").writeBytes(ByteArray(8))
            val cache = ArtworkDiskCache(cacheRoot)

            assertEquals(20L, cache.sizeBytes())
            cache.clear()

            assertEquals(0L, cache.sizeBytes())
            assertTrue(artworkDirectory.listFiles().isNullOrEmpty())
        } finally {
            cacheRoot.deleteRecursively()
        }
    }
}