package com.localmusic.player.settings

import android.content.SharedPreferences
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.SortOrder

/** Persistent defaults applied when the library ViewModel is created. */
class LibraryPreferences(private val sharedPreferences: SharedPreferences) {
    fun defaultFilter(): LibraryFilter =
            sharedPreferences.getString(KEY_DEFAULT_FILTER, null)?.let { name ->
                LibraryFilter.entries.firstOrNull { it.name == name }
            }
                    ?: LibraryFilter.AllSongs

    fun defaultSortOrder(): SortOrder =
            sharedPreferences.getString(KEY_DEFAULT_SORT_ORDER, null)?.let { name ->
                SortOrder.entries.firstOrNull { it.name == name }
            }
                    ?: SortOrder.NewestAdded

    fun setDefaultFilter(filter: LibraryFilter) {
        sharedPreferences.edit().putString(KEY_DEFAULT_FILTER, filter.name).apply()
    }

    fun setDefaultSortOrder(sortOrder: SortOrder) {
        sharedPreferences.edit().putString(KEY_DEFAULT_SORT_ORDER, sortOrder.name).apply()
    }

    private companion object {
        const val KEY_DEFAULT_FILTER = "default_library_filter"
        const val KEY_DEFAULT_SORT_ORDER = "default_library_sort_order"
    }
}
