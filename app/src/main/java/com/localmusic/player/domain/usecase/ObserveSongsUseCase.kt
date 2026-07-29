package com.localmusic.player.domain.usecase

import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.repository.MusicRepository
import com.localmusic.player.domain.repository.PlaybackController
import com.localmusic.player.domain.repository.RepeatMode
import kotlinx.coroutines.flow.Flow

/** Observes the merged, de-duplicated local music library. */
class ObserveSongsUseCase(private val repository: MusicRepository) {
    operator fun invoke(): Flow<List<Song>> = repository.observeSongs()
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

class StartPlaybackUseCase(private val playbackController: PlaybackController) {
    fun observePlayback() = playbackController.observePlayback()
    operator fun invoke(songs: List<Song>, startSongId: String) =
            playbackController.play(songs, startSongId)
    fun enqueue(song: Song) = playbackController.enqueue(song)
    fun clearQueue() = playbackController.clearQueue()
    fun resume() = playbackController.resume()
    fun pause() = playbackController.pause()
    fun seekTo(progress: Float) = playbackController.seekTo(progress)
    fun setShuffleEnabled(enabled: Boolean) = playbackController.setShuffleEnabled(enabled)
    fun setRepeatMode(mode: RepeatMode) = playbackController.setRepeatMode(mode)
    fun setVisualizerEnabled(enabled: Boolean) = playbackController.setVisualizerEnabled(enabled)
}
