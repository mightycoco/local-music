package com.localmusic.player.data.repository

import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Initial repository implementation used until MediaStore and SAF sources are wired in. */
class InMemoryMusicRepository : MusicRepository {
    private val songs = MutableStateFlow<List<Song>>(emptyList())

    override fun observeSongs(): Flow<List<Song>> = songs

    override suspend fun refreshLibrary() = Unit
}
