package com.localmusic.player.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists") suspend fun all(): List<PlaylistEntity>

    @Upsert suspend fun upsert(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE name = :name") suspend fun delete(name: String)
}
