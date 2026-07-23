package com.localmusic.player.data.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room database for local library metadata and user-owned music state. */
@Database(
    entities = [SongEntity::class],
    version = 2,
    exportSchema = true
)
abstract class LocalMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE songs ADD COLUMN genre TEXT NOT NULL DEFAULT 'Unknown Genre'"
                )
            }
        }
    }
}
