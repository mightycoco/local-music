package com.localmusic.player.artwork

import android.net.Uri
import java.io.File

/** Small disk cache for extracted album artwork bytes. */
class ArtworkDiskCache(
    cacheRoot: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val pruner: ArtworkCachePruner = ArtworkCachePruner()
) {
    private val artworkDirectory = File(cacheRoot, "artwork")

    fun get(key: String): Uri? {
        val file = fileFor(key)
        if (!file.exists()) return null

        file.setLastModified(System.currentTimeMillis())
        return Uri.fromFile(file)
    }

    fun put(key: String, bytes: ByteArray): Uri {
        artworkDirectory.mkdirs()
        val file = fileFor(key)
        file.writeBytes(bytes)
        prune()
        return Uri.fromFile(file)
    }

    fun prune() {
        val files = artworkDirectory.listFiles().orEmpty().filter { it.isFile }
        val entries = files.map { file ->
            ArtworkCacheEntry(
                key = file.nameWithoutExtension,
                sizeBytes = file.length(),
                lastModifiedEpochMillis = file.lastModified()
            )
        }
        val deleteKeys = pruner.entriesToDelete(entries, maxBytes).map { it.key }.toSet()
        files.filter { it.nameWithoutExtension in deleteKeys }.forEach { it.delete() }
    }

    private fun fileFor(key: String): File = File(artworkDirectory, "$key.art")

    private companion object {
        const val DEFAULT_MAX_BYTES = 50L * 1024L * 1024L
    }
}