package com.localmusic.player.domain.usecase

import androidx.paging.PagingData
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.LibraryFacet
import com.localmusic.player.domain.model.LibraryQuery
import com.localmusic.player.domain.model.RadioStation
import com.localmusic.player.domain.model.StreamStation
import com.localmusic.player.domain.repository.MusicRepository
import com.localmusic.player.domain.repository.PlaybackController
import com.localmusic.player.domain.repository.RadioStationDirectory
import com.localmusic.player.domain.repository.RepeatMode
import com.localmusic.player.domain.repository.StreamSourceResolver
import kotlinx.coroutines.flow.Flow

/** Observes one bounded, storage-filtered library result window. */
class ObserveLibraryPageUseCase(private val repository: MusicRepository) {
    operator fun invoke(query: LibraryQuery): Flow<List<Song>> = repository.observeLibrary(query)

    fun paged(query: LibraryQuery): Flow<PagingData<Song>> = repository.pageLibrary(query)

    fun facets(query: LibraryQuery): Flow<List<LibraryFacet>> = repository.observeLibraryFacets(query)
}

class GetSongsByUrisUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(uris: List<String>): List<Song> = repository.songsByUris(uris)
}

/** Refreshes local music metadata from configured device sources. */
class RefreshMusicLibraryUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke() = repository.refreshLibrary()
}

class AddFolderSourceUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(folderUri: String) = repository.addFolderSource(folderUri)
}

class RemoveFolderSourceUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(folderUri: String) = repository.removeFolderSource(folderUri)
}

class SetFavouriteUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(songId: String, isFavourite: Boolean) =
        repository.setFavourite(songId, isFavourite)
}

class ImportStreamSourceUseCase(
    private val resolver: StreamSourceResolver,
    private val repository: MusicRepository
) {
    suspend operator fun invoke(sourceUrl: String): List<Song> =
        repository.upsertStreamStations(resolver.resolve(sourceUrl))
}

class SearchRadioStationsUseCase(private val directory: RadioStationDirectory) {
    suspend operator fun invoke(query: String) = directory.search(query)
}

class SaveRadioStationUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(station: RadioStation): Song =
        repository.upsertStreamStations(listOf(station.toStreamStation())).first()
}

class UpdateStreamMetadataUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(songId: String, title: String, artist: String) =
        repository.updateStreamMetadata(songId, title, artist)
}

class StartPlaybackUseCase(private val playbackController: PlaybackController) {
    fun observePlayback() = playbackController.observePlayback()
    operator fun invoke(songs: List<Song>, startSongId: String) =
        playbackController.play(songs, startSongId)

    fun enqueue(song: Song) = playbackController.enqueue(song)
    fun clearQueue() = playbackController.clearQueue()
    fun resume() = playbackController.resume()
    fun pause() = playbackController.pause()
    fun skipToNext() = playbackController.skipToNext()
    fun skipToPrevious() = playbackController.skipToPrevious()
    fun seekTo(progress: Float) = playbackController.seekTo(progress)
    fun setShuffleEnabled(enabled: Boolean) = playbackController.setShuffleEnabled(enabled)
    fun setRepeatMode(mode: RepeatMode) = playbackController.setRepeatMode(mode)
    fun setVisualizerEnabled(enabled: Boolean) = playbackController.setVisualizerEnabled(enabled)
    fun close() = playbackController.close()
}
