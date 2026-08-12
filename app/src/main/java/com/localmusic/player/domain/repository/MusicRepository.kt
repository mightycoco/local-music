package com.localmusic.player.domain.repository

import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.StreamStation
import kotlinx.coroutines.flow.Flow

/** Boundary for the common music library; UI never talks to storage or MediaStore directly. */
interface MusicRepository {
    fun observeSongs(): Flow<List<Song>>

    suspend fun refreshLibrary()

    suspend fun addFolderSource(folderUri: String)

    suspend fun removeFolderSource(folderUri: String)

    suspend fun setFavourite(songId: String, isFavourite: Boolean)

    suspend fun upsertStreamStations(stations: List<StreamStation>): List<Song>

    suspend fun updateStreamMetadata(songId: String, title: String, artist: String)
}
