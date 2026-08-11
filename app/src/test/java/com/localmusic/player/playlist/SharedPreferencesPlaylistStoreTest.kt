package com.localmusic.player.playlist

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedPreferencesPlaylistStoreTest {
    @Test
    fun savePersistsPlaylistsAcrossStoreInstances() = runTest {
        val preferences = FakeSharedPreferences()
        val playlist = testPlaylist(name = "Road")

        SharedPreferencesPlaylistStore(preferences).save(playlist)

        val restored = SharedPreferencesPlaylistStore(preferences).playlists()
        assertEquals(listOf(playlist), restored)
    }

    @Test
    fun saveReplacesPlaylistWithSameName() = runTest {
        val store = SharedPreferencesPlaylistStore(FakeSharedPreferences())

        store.save(testPlaylist(name = "Road", uri = "content://music/old"))
        store.save(testPlaylist(name = "Road", uri = "content://music/new"))

        val restored = store.playlists()
        assertEquals(1, restored.size)
        assertEquals("content://music/new", restored.first().entries.first().uri)
    }

    @Test
    fun deleteRemovesPlaylistByName() = runTest {
        val store = SharedPreferencesPlaylistStore(FakeSharedPreferences())
        store.save(testPlaylist(name = "Road"))
        store.save(testPlaylist(name = "Focus"))

        store.delete("Road")

        assertEquals(listOf("Focus"), store.playlists().map { it.name })
    }

    @Test
    fun playlistsRestoresQueueBeforeAlphabeticalPlaylists() = runTest {
        val preferences = FakeSharedPreferences()
        val store = SharedPreferencesPlaylistStore(preferences)
        store.save(testPlaylist(name = "Road"))
        store.save(testPlaylist(name = M3uPlaylist.QUEUE_NAME))
        store.save(testPlaylist(name = "Focus"))

        val restored = SharedPreferencesPlaylistStore(preferences).playlists()

        assertEquals(listOf(M3uPlaylist.QUEUE_NAME, "Focus", "Road"), restored.map { it.name })
    }

    private fun testPlaylist(name: String, uri: String = "content://music/song-1"): M3uPlaylist =
            M3uPlaylist(
                    name = name,
                    entries =
                            listOf(
                                    M3uPlaylistEntry(
                                            title = "The Title",
                                            artist = "The Artist",
                                            durationSeconds = 181,
                                            uri = uri
                                    )
                            )
            )
}
