package com.localmusic.player.domain.usecase

import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveSongsUseCaseTest {
    @Test
    fun observeSongsDelegatesToRepository() = runTest {
        val expected = listOf(
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

        useCase().collect { songs ->
            assertEquals(expected, songs)
        }
    }

    private class FakeMusicRepository(
        private val songs: List<Song>
    ) : MusicRepository {
        override fun observeSongs(): Flow<List<Song>> = flowOf(songs)

        override suspend fun refreshLibrary() = Unit
    }
}
