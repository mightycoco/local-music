package com.localmusic.player.artwork

data class ArtworkCacheEntry(
    val key: String,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long
)