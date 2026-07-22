package com.localmusic.player.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Room access object for cached local song metadata. */
@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY dateAddedEpochSeconds DESC")
    fun observeNewestAdded(): Flow<List<SongEntity>>

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<String>)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query("UPDATE songs SET isFavourite = :isFavourite WHERE id = :songId")
    suspend fun setFavourite(songId: String, isFavourite: Boolean)

    @Transaction
    suspend fun replaceScannedLibrary(songs: List<SongEntity>) {
        if (songs.isEmpty()) {
            deleteAll()
            return
        }

        upsertAll(songs)
        deleteMissing(songs.map { it.id })
    }
}
