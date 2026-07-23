package com.localmusic.player.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persisted song metadata used for fast library queries and playback history. */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationMillis: Long,
    val dateAddedEpochSeconds: Long,
    val folderName: String,
    val uri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val playCount: Int,
    val lastPlayedEpochMillis: Long?,
    val isFavourite: Boolean
)
