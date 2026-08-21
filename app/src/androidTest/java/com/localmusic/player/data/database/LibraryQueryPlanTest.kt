package com.localmusic.player.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.LibraryQuery
import com.localmusic.player.domain.model.SortOrder
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryQueryPlanTest {
    private lateinit var database: LocalMusicDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LocalMusicDatabase::class.java).build()
        seedSongs(50_000)
        database.openHelper.writableDatabase.execSQL("ANALYZE")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun supportedSortsUseTheirOrderingIndexes() {
        val expectedIndexes =
            mapOf(
                SortOrder.NewestAdded to "index_songs_dateAddedEpochSeconds",
                SortOrder.DateAdded to "index_songs_dateAddedEpochSeconds",
                SortOrder.Name to "index_songs_title_artist",
                SortOrder.Artist to "index_songs_artist_title",
                SortOrder.Album to "index_songs_album_title",
                SortOrder.Duration to "index_songs_durationMillis_title",
                SortOrder.RecentlyPlayed to "index_songs_lastPlayedEpochMillis_dateAddedEpochSeconds",
                SortOrder.MostPlayed to "index_songs_playCount_dateAddedEpochSeconds"
            )

        expectedIndexes.forEach { (sortOrder, expectedIndex) ->
            val plan = explain(LibraryQuery(sortOrder = sortOrder).toSqlQuery())
            assertTrue("$sortOrder did not use $expectedIndex: $plan", plan.uses(expectedIndex))
            assertFalse("$sortOrder performed a full temporary sort: $plan", plan.usesFullTempSort())
        }
    }

    @Test
    fun browseAndFacetQueriesUseFacetIndexes() {
        val expectedIndexes =
            mapOf(
                LibraryFilter.Artists to "index_songs_artist_title",
                LibraryFilter.Albums to "index_songs_album_title",
                LibraryFilter.Genres to "index_songs_genre_title",
                LibraryFilter.Folders to "index_songs_folderName_title"
            )

        expectedIndexes.forEach { (filter, expectedIndex) ->
            val browsePlan =
                explain(
                    LibraryQuery(filter = filter, browseValue = "Value 1", sortOrder = SortOrder.Name)
                        .toSqlQuery()
                )
            val facetPlan = explain(LibraryQuery(filter = filter).toFacetSqlQuery())
            assertTrue("$filter browse did not use $expectedIndex: $browsePlan", browsePlan.uses(expectedIndex))
            assertTrue("$filter facets did not use $expectedIndex: $facetPlan", facetPlan.uses(expectedIndex))
        }
    }

    @Test
    fun containsSearchDocumentsItsIntentionalTableScan() {
        val plan = explain(LibraryQuery(searchQuery = "needle").toSqlQuery())
        assertTrue("Contains search unexpectedly stopped scanning songs: $plan", plan.any { "SCAN songs" in it })
    }

    private fun explain(query: SupportSQLiteQuery): List<String> {
        val cursor = database.openHelper.writableDatabase.query(ExplainQuery(query))
        return cursor.use {
            buildList {
                while (it.moveToNext()) add(it.getString(3))
            }
        }
    }

    private fun seedSongs(count: Int) {
        val sqlite = database.openHelper.writableDatabase
        val insert =
            sqlite.compileStatement(
                """
                INSERT INTO songs (
                    id, fileName, title, artist, album, genre, albumArtist, composer, year,
                    comments, description, durationMillis, dateAddedEpochSeconds, folderName,
                    uri, mimeType, sizeBytes, playCount, lastPlayedEpochMillis, isFavourite,
                    scanGeneration, sourceType, artworkUri
                ) VALUES (?, ?, ?, ?, ?, ?, '', '', NULL, '', '', ?, ?, ?, ?, 'audio/mpeg', ?, ?, ?, ?, 1, 'LOCAL', NULL)
                """.trimIndent()
            )
        sqlite.beginTransaction()
        try {
            repeat(count) { index ->
                val id = index.toString()
                insert.clearBindings()
                insert.bindString(1, id)
                insert.bindString(2, "track-$id.mp3")
                insert.bindString(3, "Title ${index % 10_000}")
                insert.bindString(4, "Value ${index % 500}")
                insert.bindString(5, "Value ${index % 1_000}")
                insert.bindString(6, "Value ${index % 50}")
                insert.bindLong(7, 120_000L + index)
                insert.bindLong(8, 1_700_000_000L + index)
                insert.bindString(9, "Value ${index % 100}")
                insert.bindString(10, "content://songs/$id")
                insert.bindLong(11, 4_000_000L + index)
                insert.bindLong(12, (index % 100).toLong())
                insert.bindLong(13, 1_700_000_000_000L + index)
                insert.bindLong(14, if (index % 10 == 0) 1 else 0)
                insert.executeInsert()
            }
            sqlite.setTransactionSuccessful()
        } finally {
            sqlite.endTransaction()
        }
    }
}

private class ExplainQuery(private val delegate: SupportSQLiteQuery) : SupportSQLiteQuery {
    override val sql: String = "EXPLAIN QUERY PLAN ${delegate.sql}"
    override val argCount: Int = delegate.argCount

    override fun bindTo(statement: SupportSQLiteProgram) {
        delegate.bindTo(statement)
    }
}

private fun List<String>.uses(indexName: String): Boolean = any { indexName in it }

private fun List<String>.usesFullTempSort(): Boolean = any { "USE TEMP B-TREE FOR ORDER BY" in it }