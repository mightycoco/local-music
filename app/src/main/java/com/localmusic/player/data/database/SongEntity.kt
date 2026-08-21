package com.localmusic.player.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Persisted song metadata used for fast library queries and playback history. */
@Entity(
    tableName = "songs",
    indices = [
        Index("dateAddedEpochSeconds"),
        Index("scanGeneration"),
        Index("uri", unique = true),
        Index(value = ["isFavourite", "dateAddedEpochSeconds"]),
        Index(value = ["playCount", "dateAddedEpochSeconds"]),
        Index(value = ["lastPlayedEpochMillis", "dateAddedEpochSeconds"]),
        Index(value = ["artist", "title"]),
        Index(value = ["album", "title"]),
        Index(value = ["genre", "title"]),
        Index(value = ["folderName", "title"]),
        Index(value = ["title", "artist"]),
        Index(value = ["durationMillis", "title"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val title: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val artist: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val album: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val genre: String,
    val albumArtist: String,
    val composer: String,
    val year: Int?,
    val comments: String,
    val description: String,
    val durationMillis: Long,
    val dateAddedEpochSeconds: Long,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val folderName: String,
    val uri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val playCount: Int,
    val lastPlayedEpochMillis: Long?,
    val isFavourite: Boolean,
    val scanGeneration: Long = 0L,
    val sourceType: String = "LOCAL",
    val artworkUri: String? = null
)
