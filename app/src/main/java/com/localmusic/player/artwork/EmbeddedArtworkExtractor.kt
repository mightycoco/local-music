package com.localmusic.player.artwork

import android.content.Context
import android.media.MediaMetadataRetriever
import com.localmusic.player.domain.model.Song

/** Extracts embedded artwork from local audio metadata and stores it in the artwork cache. */
class EmbeddedArtworkExtractor(
    private val context: Context,
    private val cache: ArtworkDiskCache,
    private val lookup: ArtworkLookup = ArtworkLookup()
) {
    fun artworkFor(song: Song): android.net.Uri? {
        val key = lookup.cacheKey(song)
        cache.get(key)?.let { return it }

        val bytes = MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, lookup.embeddedArtworkCandidate(song))
            retriever.embeddedPicture
        } ?: return null

        return cache.put(key, bytes)
    }
}