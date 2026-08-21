package com.localmusic.player.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

/** Room access object for cached local song metadata. */
@Dao
interface SongDao {
    @RawQuery(observedEntities = [SongEntity::class])
    fun observeSongs(query: SupportSQLiteQuery): Flow<List<SongEntity>>

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM songs")
    suspend fun allSongs(): List<SongEntity>

    @Query("DELETE FROM songs WHERE sourceType = 'LOCAL' AND scanGeneration != :generation")
    suspend fun deleteBeforeGeneration(generation: Long)

    @Query("SELECT * FROM songs WHERE uri IN (:uris)")
    suspend fun songsByUris(uris: List<String>): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun songsByIds(ids: List<String>): List<SongEntity>

    @Query("UPDATE songs SET title = :title, artist = :artist WHERE id = :songId AND sourceType = 'STREAM'")
    suspend fun updateStreamMetadata(songId: String, title: String, artist: String)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

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
