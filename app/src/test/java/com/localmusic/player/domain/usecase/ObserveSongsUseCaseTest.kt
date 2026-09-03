package com.localmusic.player.domain.usecase

import com.localmusic.player.domain.model.LibraryQuery
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.StreamStation
import com.localmusic.player.domain.repository.MusicRepository
import com.localmusic.player.domain.repository.PlaybackController
import com.localmusic.player.domain.repository.PlaybackSnapshot
import com.localmusic.player.domain.repository.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveSongsUseCaseTest {
    @Test
    fun observeLibraryDelegatesToRepository() = runTest {
        val expected =
            listOf(
                Song(
                    id = "song-1",
                    title = "A Song",
                    artist = "An Artist",
                    album = "An Album",
                    durationMillis = 180_000,
                    dateAddedEpochSeconds = 1_700_000_000,
                    folderName = "Music",
                    uri = "content://media/song-1"
                )
            )
        val useCase = ObserveLibraryPageUseCase(FakeMusicRepository(expected))

        useCase(LibraryQuery()).collect { songs -> assertEquals(expected, songs) }
    }

    @Test
    fun addFolderSourceDelegatesToRepository() = runTest {
        val repository = FakeMusicRepository(emptyList())
        val useCase = AddFolderSourceUseCase(repository)

        useCase("content://tree/music")

        assertEquals("content://tree/music", repository.addedFolderUri)
    }

    @Test
    fun removeFolderSourceDelegatesToRepository() = runTest {
        val repository = FakeMusicRepository(emptyList())
        val useCase = RemoveFolderSourceUseCase(repository)

        useCase("content://tree/music")

        assertEquals("content://tree/music", repository.removedFolderUri)
    }

    @Test
    fun setFavouriteDelegatesToRepository() = runTest {
        val repository = FakeMusicRepository(emptyList())
        val useCase = SetFavouriteUseCase(repository)

        useCase("song-1", true)

        assertEquals("song-1" to true, repository.favouriteUpdate)
    }

    @Test
    fun startPlaybackDelegatesToPlaybackController() = runTest {
        val songs = listOf(testSong("song-1"), testSong("song-2"))
        val playbackController = FakePlaybackController()
        val useCase = StartPlaybackUseCase(playbackController)

        useCase(songs, "song-2")

        assertEquals(songs to "song-2", playbackController.startedPlayback)
    }

    @Test
    fun queueCommandsDelegateToPlaybackController() {
        val song = testSong("song-1")
        val playbackController = FakePlaybackController()
        val useCase = StartPlaybackUseCase(playbackController)

        useCase.enqueue(song)
        useCase.clearQueue()

        assertEquals(song, playbackController.enqueuedSong)
        assertEquals(true, playbackController.queueWasCleared)
    }

    @Test
    fun playbackModeCommandsDelegateToPlaybackController() {
        val playbackController = FakePlaybackController()
        val useCase = StartPlaybackUseCase(playbackController)

        useCase.setShuffleEnabled(true)
        useCase.setRepeatMode(RepeatMode.All)
        useCase.skipToNext()
        useCase.skipToPrevious()

        assertEquals(true, playbackController.shuffleEnabled)
        assertEquals(RepeatMode.All, playbackController.recordedRepeatMode)
        assertEquals(1, playbackController.nextCount)
        assertEquals(1, playbackController.previousCount)
    }

    private class FakeMusicRepository(private val songs: List<Song>) : MusicRepository {
        var addedFolderUri: String? = null
        var removedFolderUri: String? = null
        var favouriteUpdate: Pair<String, Boolean>? = null

        override fun observeLibrary(query: LibraryQuery): Flow<List<Song>> = flowOf(songs)

        override fun pageLibrary(query: LibraryQuery) =
            throw UnsupportedOperationException("Not used by this test")

        override fun observeLibraryFacets(query: LibraryQuery) =
            throw UnsupportedOperationException("Not used by this test")

        override suspend fun songsByUris(uris: List<String>): List<Song> = emptyList()

        override suspend fun songsByIds(ids: List<String>): List<Song> = emptyList()

        override suspend fun refreshLibrary() = Unit

        override suspend fun addFolderSource(folderUri: String) {
            addedFolderUri = folderUri
        }

        override suspend fun removeFolderSource(folderUri: String) {
            removedFolderUri = folderUri
        }

        override suspend fun setFavourite(songId: String, isFavourite: Boolean) {
            favouriteUpdate = songId to isFavourite
        }

        override suspend fun upsertStreamStations(stations: List<StreamStation>): List<Song> =
            emptyList()

        override suspend fun updateStreamMetadata(songId: String, title: String, artist: String) = Unit
    }

    private class FakePlaybackController : PlaybackController {
        var startedPlayback: Pair<List<Song>, String>? = null
        var enqueuedSong: Song? = null
        var queueWasCleared = false
        var shuffleEnabled: Boolean? = null
        var recordedRepeatMode: RepeatMode? = null
        var nextCount = 0
        var previousCount = 0

        override fun observePlayback(): Flow<PlaybackSnapshot> = flowOf(PlaybackSnapshot())

        override fun play(songs: List<Song>, startSongId: String) {
            startedPlayback = songs to startSongId
        }

        override fun enqueue(song: Song) {
            enqueuedSong = song
        }

        override fun clearQueue() {
            queueWasCleared = true
        }

        override fun resume() = Unit

        override fun pause() = Unit

        override fun skipToNext() {
            nextCount++
        }

        override fun skipToPrevious() {
            previousCount++
        }

        override fun seekTo(progress: Float) = Unit

        override fun setShuffleEnabled(enabled: Boolean) {
            shuffleEnabled = enabled
        }

        override fun setRepeatMode(mode: RepeatMode) {
            recordedRepeatMode = mode
        }

        override fun setVisualizerEnabled(enabled: Boolean) = Unit

        override fun close() = Unit
    }

    private fun testSong(id: String): Song =
        Song(
            id = id,
            title = "A Song",
            artist = "An Artist",
            album = "An Album",
            durationMillis = 180_000,
            dateAddedEpochSeconds = 1_700_000_000,
            folderName = "Music",
            uri = "content://media/$id"
        )
}
