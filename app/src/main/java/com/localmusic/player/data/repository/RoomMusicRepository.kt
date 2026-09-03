package com.localmusic.player.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.localmusic.player.data.database.SongDao
import com.localmusic.player.data.database.SongEntity
import com.localmusic.player.data.database.toSqlQuery
import com.localmusic.player.data.database.toFacetSqlQuery
import com.localmusic.player.data.database.toDomain
import com.localmusic.player.data.database.toEntity
import com.localmusic.player.data.mediastore.MusicScanner
import com.localmusic.player.data.saf.SafFolderSourceStore
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.LibraryQuery
import com.localmusic.player.domain.model.LibraryFacet
import com.localmusic.player.domain.model.SongSource
import com.localmusic.player.domain.model.StreamStation
import com.localmusic.player.domain.repository.MusicRepository
import java.util.concurrent.atomic.AtomicLong
import java.security.MessageDigest
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
    override fun observeLibrary(query: LibraryQuery): Flow<List<Song>> =
        songDao.observeSongs(query.toSqlQuery()).map { entities -> entities.map { it.toDomain() } }

    override fun pageLibrary(query: LibraryQuery): Flow<PagingData<Song>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = LibraryQuery.DEFAULT_PAGE_SIZE,
                    prefetchDistance = LIBRARY_PREFETCH_DISTANCE,
                    initialLoadSize = LibraryQuery.DEFAULT_PAGE_SIZE,
                    maxSize = LIBRARY_MAX_CACHED_SONGS,
                    enablePlaceholders = true
                ),
            pagingSourceFactory = { songDao.pageSongs(query.toSqlQuery(includeWindow = false)) }
        ).flow.map { pagingData -> pagingData.map(SongEntity::toDomain) }

    override fun observeLibraryFacets(query: LibraryQuery): Flow<List<LibraryFacet>> =
        songDao.observeFacetCounts(query.toFacetSqlQuery()).map { rows ->
            rows.map { row -> LibraryFacet(value = row.value, songCount = row.songCount) }
        }

    override suspend fun songsByUris(uris: List<String>): List<Song> =
        withContext(ioDispatcher) { songDao.songsByUris(uris).map(SongEntity::toDomain) }

    override suspend fun songsByIds(ids: List<String>): List<Song> =
        withContext(ioDispatcher) { songDao.songsByIds(ids).map(SongEntity::toDomain) }

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

    override suspend fun upsertStreamStations(stations: List<StreamStation>): List<Song> =
        withContext(ioDispatcher) {
            if (stations.isEmpty()) return@withContext emptyList()
            val now = System.currentTimeMillis()
            val entities = stations.distinctBy(StreamStation::uri).map { station ->
                SongEntity(
                    id = "stream:${station.uri.sha256()}",
                    fileName = station.uri,
                    title = station.title,
                    artist = station.artist,
                    album = "Online streams",
                    genre = "",
                    albumArtist = "",
                    composer = "",
                    year = null,
                    comments = "",
                    description = "",
                    durationMillis = station.durationSeconds.coerceAtLeast(0) * 1_000,
                    dateAddedEpochSeconds = now / 1_000,
                    folderName = "Online streams",
                    uri = station.uri,
                    mimeType = "audio/*",
                    sizeBytes = 0L,
                    playCount = 0,
                    lastPlayedEpochMillis = null,
                    isFavourite = false,
                    scanGeneration = 0L,
                    sourceType = SongSource.STREAM.name,
                    artworkUri = station.artworkUri
                )
            }
            songDao.upsertStreamStations(entities).map(SongEntity::toDomain)
        }

    override suspend fun updateStreamMetadata(songId: String, title: String, artist: String) =
        withContext(ioDispatcher) { songDao.updateStreamMetadata(songId, title, artist) }
}

private const val LIBRARY_PREFETCH_DISTANCE = 20
private const val LIBRARY_MAX_CACHED_SONGS = 500

private fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

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
