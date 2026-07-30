package com.localmusic.player.settings

import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.playlist.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryPreferencesTest {
    @Test
    fun defaultsUseAllSongsAndNewestAddedWithoutStoredValues() {
        val preferences = LibraryPreferences(FakeSharedPreferences())

        assertEquals(LibraryFilter.AllSongs, preferences.defaultFilter())
        assertEquals(SortOrder.NewestAdded, preferences.defaultSortOrder())
    }

    @Test
    fun selectedDefaultsArePersisted() {
        val preferences = LibraryPreferences(FakeSharedPreferences())

        preferences.setDefaultFilter(LibraryFilter.MostPlayed)
        preferences.setDefaultSortOrder(SortOrder.Artist)

        assertEquals(LibraryFilter.MostPlayed, preferences.defaultFilter())
        assertEquals(SortOrder.Artist, preferences.defaultSortOrder())
    }

    @Test
    fun invalidStoredDefaultsFallBackToBuiltInValues() {
        val sharedPreferences = FakeSharedPreferences()
        sharedPreferences
                .edit()
                .putString("default_library_filter", "Unknown")
                .putString("default_library_sort_order", "Unknown")
                .apply()

        val preferences = LibraryPreferences(sharedPreferences)

        assertEquals(LibraryFilter.AllSongs, preferences.defaultFilter())
        assertEquals(SortOrder.NewestAdded, preferences.defaultSortOrder())
    }
}
