package com.localmusic.player.artwork

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.localmusic.player.domain.model.Song

/** Resolves local-first artwork and caches optional online artwork for local songs. */
class EmbeddedArtworkExtractor(
        private val context: Context,
        private val cache: ArtworkDiskCache,
        private val lookup: ArtworkLookup = ArtworkLookup(),
        private val externalArtworkProvider: ExternalArtworkProvider = MusicBrainzCoverArtProvider()
) {
    fun artworkFor(song: Song, allowExternalDownload: Boolean): Uri? {
        localArtworkFor(song)?.let {
            return it
        }
        if (!allowExternalDownload) return null

        return downloadArtworkFor(song)
    }

    /** Resolves embedded, sibling, and previously downloaded artwork without using the network. */
    fun localArtworkFor(song: Song): Uri? {
        val key = lookup.cacheKey(song)
        embeddedArtwork(song)?.let {
            return cache.put(key, it)
        }
        siblingArtwork(song, "folder.jpg")?.let {
            return cache.put(key, it)
        }
        siblingArtwork(song, "cover.jpg")?.let {
            return cache.put(key, it)
        }
        return cache.get(key)
    }

    /**
     * Downloads artwork only after [localArtworkFor] has established no local image is available.
     */
    fun downloadArtworkFor(song: Song): Uri? {
        val key = lookup.cacheKey(song)
        cache.get(key)?.let {
            return it
        }
        return externalArtworkProvider.artworkFor(song)?.let { bytes -> cache.put(key, bytes) }
    }

    private fun embeddedArtwork(song: Song): ByteArray? =
            runCatching {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, lookup.embeddedArtworkCandidate(song))
                            retriever.embeddedPicture?.takeIf { it.size <= MAX_ARTWORK_BYTES }
                        } finally {
                            retriever.release()
                        }
                    }
                    .getOrNull()

    private fun siblingArtwork(song: Song, fileName: String): ByteArray? =
            artworkDocument(song, fileName)?.let(context.contentResolver::openInputStream)?.use {
                it.readBytesLimited(MAX_ARTWORK_BYTES)
            }

    private fun java.io.InputStream.readBytesLimited(maxBytes: Int): ByteArray? {
        val output = java.io.ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) return output.toByteArray()
            total += read
            if (total > maxBytes) return null
            output.write(buffer, 0, read)
        }
    }

    private fun artworkDocument(song: Song, fileName: String): Uri? =
            when (Uri.parse(song.uri).scheme) {
                "content" -> safArtworkDocument(song, fileName)
                                ?: mediaStoreArtworkDocument(song, fileName)
                else -> null
            }

    private fun safArtworkDocument(song: Song, fileName: String): Uri? =
            runCatching {
                        DocumentFile.fromSingleUri(context, Uri.parse(song.uri))
                                ?.parentFile
                                ?.findFile(fileName)
                                ?.takeIf { it.isFile }
                                ?.uri
                    }
                    .getOrNull()

    private fun mediaStoreArtworkDocument(song: Song, fileName: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !song.id.startsWith("mediastore:"))
                return null
        val relativePath =
                context.contentResolver
                        .query(
                                Uri.parse(song.uri),
                                arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
                                null,
                                null,
                                null
                        )
                        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                        ?.takeIf { it.isNotBlank() }
                        ?: return null

        return context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Images.Media._ID),
                        "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                        arrayOf(relativePath, fileName),
                        null
                )
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                cursor.getLong(0).toString()
                        )
                    } else {
                        null
                    }
                }
    }

    private companion object {
        const val MAX_ARTWORK_BYTES = 8 * 1024 * 1024
    }
}
