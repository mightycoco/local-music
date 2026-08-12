package com.localmusic.player.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room database for local library metadata and user-owned music state. */
@Database(entities = [SongEntity::class, PlaylistEntity::class], version = 6, exportSchema = true)
abstract class LocalMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE songs ADD COLUMN genre TEXT NOT NULL DEFAULT 'Unknown Genre'"
                    )
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE songs ADD COLUMN scanGeneration INTEGER NOT NULL DEFAULT 0"
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_songs_dateAddedEpochSeconds ON songs(dateAddedEpochSeconds)"
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_songs_scanGeneration ON songs(scanGeneration)"
                    )
                }
            }

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS playlists (name TEXT NOT NULL, content TEXT NOT NULL, PRIMARY KEY(name))"
                    )
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE songs ADD COLUMN albumArtist TEXT NOT NULL DEFAULT ''"
                    )
                    database.execSQL(
                        "ALTER TABLE songs ADD COLUMN composer TEXT NOT NULL DEFAULT ''"
                    )
                    database.execSQL("ALTER TABLE songs ADD COLUMN year INTEGER")
                    database.execSQL(
                        "ALTER TABLE songs ADD COLUMN comments TEXT NOT NULL DEFAULT ''"
                    )
                    database.execSQL(
                        "ALTER TABLE songs ADD COLUMN description TEXT NOT NULL DEFAULT ''"
                    )
                }
            }

        val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE songs ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'LOCAL'"
                    )
                    database.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_songs_uri ON songs(uri)"
                    )
                }
            }
    }
}
