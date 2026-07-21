package com.localmusic.player.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/** Room database for local library metadata and user-owned music state. */
@Database(
    entities = [SongEntity::class],
    version = 1,
    exportSchema = true
)
abstract class LocalMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
