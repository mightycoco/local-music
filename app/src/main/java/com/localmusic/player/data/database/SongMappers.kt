package com.localmusic.player.data.database

import com.localmusic.player.domain.model.Song

fun SongEntity.toDomain(): Song = Song(
    id = id,
    fileName = fileName,
    title = title,
    artist = artist,
    album = album,
    durationMillis = durationMillis,
    dateAddedEpochSeconds = dateAddedEpochSeconds,
    folderName = folderName,
    uri = uri,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    playCount = playCount,
    lastPlayedEpochMillis = lastPlayedEpochMillis,
    isFavourite = isFavourite
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    fileName = fileName,
    title = title,
    artist = artist,
    album = album,
    durationMillis = durationMillis,
    dateAddedEpochSeconds = dateAddedEpochSeconds,
    folderName = folderName,
    uri = uri,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    playCount = playCount,
    lastPlayedEpochMillis = lastPlayedEpochMillis,
    isFavourite = isFavourite
)