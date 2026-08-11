package com.localmusic.player.playlist

import com.localmusic.player.data.database.PlaylistDao
import com.localmusic.player.data.database.PlaylistEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomPlaylistStoreTest {
    @Test
    fun playlistsMigratesLegacyDataWhenRoomIsEmpty() = runTest {
        val dao = FakePlaylistDao()
        val legacyStore = InMemoryPlaylistStore(listOf(testPlaylist("Road")))

        val playlists = RoomPlaylistStore(dao, legacyStore).playlists()

        assertEquals(listOf("Road"), playlists.map { it.name })
        assertEquals(listOf("Road"), dao.all().map { it.name })
    }

    @Test
    fun playlistsDoesNotMergeLegacyDataWhenRoomHasRows() = runTest {
        val codec = M3uPlaylistCodec()
        val dao =
                FakePlaylistDao(listOf(PlaylistEntity("Room", codec.export(testPlaylist("Room")))))
        val legacyStore = InMemoryPlaylistStore(listOf(testPlaylist("Legacy")))

        val playlists = RoomPlaylistStore(dao, legacyStore).playlists()

        assertEquals(listOf("Room"), playlists.map { it.name })
    }

    private fun testPlaylist(name: String) =
            M3uPlaylist(
                    name = name,
                    entries =
                            listOf(
                                    M3uPlaylistEntry(
                                            title = "Title",
                                            artist = "Artist",
                                            durationSeconds = 181,
                                            uri = "content://music/song-1"
                                    )
                            )
            )

    private class FakePlaylistDao(initial: List<PlaylistEntity> = emptyList()) : PlaylistDao {
        private val entities = initial.associateByTo(linkedMapOf()) { it.name }

        override suspend fun all(): List<PlaylistEntity> = entities.values.toList()

        override suspend fun upsert(playlist: PlaylistEntity) {
            entities[playlist.name] = playlist
        }

        override suspend fun delete(name: String) {
            entities.remove(name)
        }
    }

    private class InMemoryPlaylistStore(initial: List<M3uPlaylist>) : PlaylistStore {
        private val playlists = initial.associateByTo(linkedMapOf()) { it.name }

        override suspend fun playlists(): List<M3uPlaylist> = playlists.values.toList()

        override suspend fun save(playlist: M3uPlaylist) {
            playlists[playlist.name] = playlist
        }

        override suspend fun delete(name: String) {
            playlists.remove(name)
        }
    }
}
