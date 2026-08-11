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

    @Synchronized
    fun get(key: String): Uri? {
        val file = fileFor(key)
        if (!file.exists()) return null

        file.setLastModified(System.currentTimeMillis())
        return Uri.fromFile(file)
    }

    @Synchronized
    fun put(key: String, bytes: ByteArray): Uri? {
        if (bytes.isEmpty() || bytes.size > MAX_ARTWORK_BYTES) return null
        artworkDirectory.mkdirs()
        val file = fileFor(key)
        val temporaryFile = File.createTempFile("artwork-", ".tmp", artworkDirectory)
        try {
            temporaryFile.writeBytes(bytes)
            if (!temporaryFile.renameTo(file)) {
                file.delete()
                check(temporaryFile.renameTo(file)) { "Unable to commit artwork cache entry" }
            }
        } finally {
            temporaryFile.delete()
        }
        prune()
        return Uri.fromFile(file)
    }

    @Synchronized
    fun sizeBytes(): Long = artworkDirectory.listFiles().orEmpty().sumOf { file -> file.length() }

    @Synchronized
    fun clear() {
        artworkDirectory.listFiles().orEmpty().forEach { file -> file.delete() }
    }

    @Synchronized
    fun prune() {
        val files = artworkDirectory.listFiles().orEmpty().filter { it.isFile }
        val entries =
                files.map { file ->
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
        const val MAX_ARTWORK_BYTES = 8 * 1024 * 1024
    }
}
