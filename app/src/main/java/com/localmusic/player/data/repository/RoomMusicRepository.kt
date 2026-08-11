package com.localmusic.player.data.repository

import com.localmusic.player.data.database.SongDao
import com.localmusic.player.data.database.SongEntity
import com.localmusic.player.data.database.toDomain
import com.localmusic.player.data.database.toEntity
import com.localmusic.player.data.mediastore.MusicScanner
import com.localmusic.player.data.saf.SafFolderSourceStore
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.repository.MusicRepository
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val nextScanGeneration = AtomicLong(System.currentTimeMillis())

/** Room-backed repository for merged, de-duplicated local music metadata. */
class RoomMusicRepository(
        private val songDao: SongDao,
        private val musicScanner: MusicScanner,
        private val safFolderSourceStore: SafFolderSourceStore? = null,
        private val persistSafFolderPermission: suspend (String) -> Unit = {},
        private val duplicateSongResolver: DuplicateSongResolver = DuplicateSongResolver(),
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MusicRepository {
    override fun observeSongs(): Flow<List<Song>> =
            songDao.observeNewestAdded().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshLibrary() =
            withContext(ioDispatcher) {
                val scanResult = musicScanner.scan()
                val scannedSongs = duplicateSongResolver.resolve(scanResult.songs)
                val scannedEntities = scannedSongs.map { it.toEntity() }
                songDao.replaceScannedLibrary(
                        songs = scannedEntities,
                        generation = nextScanGeneration.incrementAndGet(),
                        deleteMissing = scanResult.isComplete
                )
            }

    override suspend fun addFolderSource(folderUri: String) =
            withContext(ioDispatcher) {
                persistSafFolderPermission(folderUri)
                safFolderSourceStore?.add(folderUri)
                refreshLibrary()
            }

    override suspend fun removeFolderSource(folderUri: String) =
            withContext(ioDispatcher) {
                if (safFolderSourceStore?.remove(folderUri) == true) {
                    refreshLibrary()
                }
            }

    override suspend fun setFavourite(songId: String, isFavourite: Boolean) =
            withContext(ioDispatcher) { songDao.setFavourite(songId, isFavourite) }
}

internal fun List<SongEntity>.mergeRetainedState(
        retainedEntities: List<SongEntity>
): List<SongEntity> {
    val retainedById = retainedEntities.associateBy { it.id }
    return map { scanned ->
        val retained = retainedById[scanned.id]
        if (retained == null) {
            scanned
        } else {
            scanned.copy(
                    playCount = retained.playCount,
                    lastPlayedEpochMillis = retained.lastPlayedEpochMillis,
                    isFavourite = retained.isFavourite
            )
        }
    }
}
