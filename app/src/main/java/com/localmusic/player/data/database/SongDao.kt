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

    @Upsert suspend fun upsertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM songs") suspend fun allSongs(): List<SongEntity>

    @Query("DELETE FROM songs WHERE scanGeneration != :generation")
    suspend fun deleteBeforeGeneration(generation: Long)

    @Query("DELETE FROM songs") suspend fun deleteAll()

    @Query("UPDATE songs SET isFavourite = :isFavourite WHERE id = :songId")
    suspend fun setFavourite(songId: String, isFavourite: Boolean)

    @Transaction
    suspend fun replaceScannedLibrary(
            songs: List<SongEntity>,
            generation: Long,
            deleteMissing: Boolean
    ) {
        val retainedById = allSongs().associateBy(SongEntity::id)
        val merged =
                songs.map { scanned ->
                    val retained = retainedById[scanned.id]
                    scanned.copy(
                            playCount = retained?.playCount ?: scanned.playCount,
                            lastPlayedEpochMillis = retained?.lastPlayedEpochMillis,
                            isFavourite = retained?.isFavourite ?: scanned.isFavourite,
                            scanGeneration = generation
                    )
                }
        upsertAll(merged)
        if (deleteMissing) deleteBeforeGeneration(generation)
    }
}
