package com.localmusic.player.domain.repository

import com.localmusic.player.domain.model.Song
import kotlinx.coroutines.flow.Flow

/** Boundary for local music library access; UI never talks to MediaStore directly. */
interface MusicRepository {
    fun observeSongs(): Flow<List<Song>>

    suspend fun refreshLibrary()

    suspend fun addFolderSource(folderUri: String)

    suspend fun setFavourite(songId: String, isFavourite: Boolean)
}
