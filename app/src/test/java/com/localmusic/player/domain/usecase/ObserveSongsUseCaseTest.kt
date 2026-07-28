package com.localmusic.player.domain.usecase

import com.localmusic.player.domain.model.Song
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
    fun observeSongsDelegatesToRepository() = runTest {
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
        val useCase = ObserveSongsUseCase(FakeMusicRepository(expected))

        useCase().collect { songs -> assertEquals(expected, songs) }
    }

    @Test
    fun addFolderSourceDelegatesToRepository() = runTest {
        val repository = FakeMusicRepository(emptyList())
        val useCase = AddFolderSourceUseCase(repository)

        useCase("content://tree/music")

        assertEquals("content://tree/music", repository.addedFolderUri)
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

        assertEquals(true, playbackController.shuffleEnabled)
        assertEquals(RepeatMode.All, playbackController.recordedRepeatMode)
    }

    private class FakeMusicRepository(private val songs: List<Song>) : MusicRepository {
        var addedFolderUri: String? = null
        var favouriteUpdate: Pair<String, Boolean>? = null

        override fun observeSongs(): Flow<List<Song>> = flowOf(songs)

        override suspend fun refreshLibrary() = Unit

        override suspend fun addFolderSource(folderUri: String) {
            addedFolderUri = folderUri
        }

        override suspend fun setFavourite(songId: String, isFavourite: Boolean) {
            favouriteUpdate = songId to isFavourite
        }
    }

    private class FakePlaybackController : PlaybackController {
        var startedPlayback: Pair<List<Song>, String>? = null
        var enqueuedSong: Song? = null
        var queueWasCleared = false
        var shuffleEnabled: Boolean? = null
        var recordedRepeatMode: RepeatMode? = null

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

        override fun seekTo(progress: Float) = Unit

        override fun setShuffleEnabled(enabled: Boolean) {
            shuffleEnabled = enabled
        }

        override fun setRepeatMode(mode: RepeatMode) {
            recordedRepeatMode = mode
        }

        override fun setVisualizerEnabled(enabled: Boolean) = Unit
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
