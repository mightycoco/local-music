package com.localmusic.player.data.database

import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SongSource

fun SongEntity.toDomain(): Song = Song(
    id = id,
    fileName = fileName,
    title = title,
    artist = artist,
    album = album,
    genre = genre,
    albumArtist = albumArtist,
    composer = composer,
    year = year,
    comments = comments,
    description = description,
    durationMillis = durationMillis,
    dateAddedEpochSeconds = dateAddedEpochSeconds,
    folderName = folderName,
    uri = uri,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    playCount = playCount,
    lastPlayedEpochMillis = lastPlayedEpochMillis,
    isFavourite = isFavourite,
    source = SongSource.entries.firstOrNull { it.name == sourceType } ?: SongSource.LOCAL,
    artworkUri = artworkUri
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    fileName = fileName,
    title = title,
    artist = artist,
    album = album,
    genre = genre,
    albumArtist = albumArtist,
    composer = composer,
    year = year,
    comments = comments,
    description = description,
    durationMillis = durationMillis,
    dateAddedEpochSeconds = dateAddedEpochSeconds,
    folderName = folderName,
    uri = uri,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    playCount = playCount,
    lastPlayedEpochMillis = lastPlayedEpochMillis,
    isFavourite = isFavourite,
    sourceType = source.name,
    artworkUri = artworkUri
)