package com.localmusic.player.domain.usecase

import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow

/** Observes the merged, de-duplicated local music library. */
class ObserveSongsUseCase(
    private val repository: MusicRepository
) {
    operator fun invoke(): Flow<List<Song>> = repository.observeSongs()
}

/** Refreshes local music metadata from configured device sources. */
class RefreshMusicLibraryUseCase(
    private val repository: MusicRepository
) {
    suspend operator fun invoke() = repository.refreshLibrary()
}
