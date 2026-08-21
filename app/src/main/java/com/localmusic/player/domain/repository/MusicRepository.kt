package com.localmusic.player.domain.repository

import com.localmusic.player.domain.model.LibraryQuery
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.StreamStation
import kotlinx.coroutines.flow.Flow

/** Boundary for the common music library; UI never talks to storage or MediaStore directly. */
interface MusicRepository {
    fun observeLibrary(query: LibraryQuery): Flow<List<Song>>

    suspend fun songsByUris(uris: List<String>): List<Song>

    suspend fun songsByIds(ids: List<String>): List<Song>

    suspend fun refreshLibrary()

    suspend fun addFolderSource(folderUri: String)

    suspend fun removeFolderSource(folderUri: String)

    suspend fun setFavourite(songId: String, isFavourite: Boolean)

    suspend fun upsertStreamStations(stations: List<StreamStation>): List<Song>

    suspend fun updateStreamMetadata(songId: String, title: String, artist: String)
}
