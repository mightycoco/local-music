package com.localmusic.player.data.database

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.LibraryQuery
import com.localmusic.player.domain.model.SortOrder

internal fun LibraryQuery.toSqlQuery(): SupportSQLiteQuery {
    val selection = mutableListOf<String>()
    val arguments = mutableListOf<Any>()

    filter.toSelection(selection, arguments)
    browseValue?.trim()?.takeIf(String::isNotEmpty)?.let { value ->
        selection += "${filter.browseColumn()} COLLATE NOCASE = ?"
        arguments += value
    }
    searchQuery.trim().takeIf(String::isNotEmpty)?.let { query ->
        val searchExpression = "%$query%"
        selection +=
            "(" +
                    SEARCH_COLUMNS.joinToString(" OR ") { "$it LIKE ? COLLATE NOCASE" } +
                    ")"
        repeat(SEARCH_COLUMNS.size) { arguments += searchExpression }
    }

    val whereClause = selection.takeIf(List<String>::isNotEmpty)?.joinToString(
        prefix = " WHERE ",
        separator = " AND "
    ).orEmpty()
    val orderBy = sortOrder.toOrderBy()
    arguments += limit
    arguments += offset
    return SimpleSQLiteQuery(
        "SELECT * FROM songs$whereClause ORDER BY $orderBy LIMIT ? OFFSET ?",
        arguments.toTypedArray()
    )
}

private fun LibraryFilter.toSelection(selection: MutableList<String>, arguments: MutableList<Any>) {
    when (this) {
        LibraryFilter.AllSongs,
        LibraryFilter.Artists,
        LibraryFilter.Albums,
        LibraryFilter.Genres,
        LibraryFilter.Folders -> Unit

        LibraryFilter.Favourites -> selection += "isFavourite = 1"
        LibraryFilter.RecentlyAdded -> selection += "dateAddedEpochSeconds > 0"
        LibraryFilter.RecentlyPlayed -> selection += "lastPlayedEpochMillis IS NOT NULL"
        LibraryFilter.MostPlayed -> selection += "playCount > 0"
        LibraryFilter.NeverPlayed -> selection += "playCount = 0"
        LibraryFilter.LastThirtyDays -> {
            selection += "lastPlayedEpochMillis >= ?"
            arguments += System.currentTimeMillis() - THIRTY_DAYS_MILLIS
        }
    }
}

private fun LibraryFilter.browseColumn(): String =
    when (this) {
        LibraryFilter.Artists -> "artist"
        LibraryFilter.Albums -> "album"
        LibraryFilter.Genres -> "genre"
        LibraryFilter.Folders -> "folderName"
        else -> error("$this does not support a browse value.")
    }

private fun SortOrder.toOrderBy(): String =
    when (this) {
        SortOrder.NewestAdded,
        SortOrder.DateAdded -> "dateAddedEpochSeconds DESC, title COLLATE NOCASE ASC"

        SortOrder.Name -> "title COLLATE NOCASE ASC, artist COLLATE NOCASE ASC"
        SortOrder.Artist -> "artist COLLATE NOCASE ASC, title COLLATE NOCASE ASC"
        SortOrder.Album -> "album COLLATE NOCASE ASC, title COLLATE NOCASE ASC"
        SortOrder.Duration -> "durationMillis DESC, title COLLATE NOCASE ASC"
        SortOrder.RecentlyPlayed -> "lastPlayedEpochMillis DESC, title COLLATE NOCASE ASC"
        SortOrder.MostPlayed -> "playCount DESC, title COLLATE NOCASE ASC"
    }

private val SEARCH_COLUMNS =
    listOf(
        "title",
        "artist",
        "album",
        "albumArtist",
        "genre",
        "composer",
        "year",
        "comments",
        "description",
        "fileName",
        "folderName"
    )

private const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1_000