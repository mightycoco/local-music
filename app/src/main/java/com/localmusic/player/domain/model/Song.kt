package com.localmusic.player.domain.model

/** Immutable domain model for a playable local audio item. */
data class Song(
    val id: String,
    val fileName: String = "",
    val title: String,
    val artist: String,
    val album: String,
    val durationMillis: Long,
    val dateAddedEpochSeconds: Long,
    val folderName: String,
    val uri: String,
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val playCount: Int = 0,
    val lastPlayedEpochMillis: Long? = null,
    val isFavourite: Boolean = false
)
