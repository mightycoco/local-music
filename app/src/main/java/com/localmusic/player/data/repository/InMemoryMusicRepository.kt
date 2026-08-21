package com.localmusic.player.data.repository

import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.LibraryQuery
import com.localmusic.player.domain.model.SongSource
import com.localmusic.player.domain.model.StreamStation
import com.localmusic.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Initial repository implementation used until MediaStore and SAF sources are wired in. */
class InMemoryMusicRepository : MusicRepository {
    private val songs = MutableStateFlow<List<Song>>(emptyList())

    override fun observeLibrary(query: LibraryQuery): Flow<List<Song>> =
        songs.map { library -> library.drop(query.offset).take(query.limit) }

    override suspend fun songsByUris(uris: List<String>): List<Song> =
        songs.value.filter { it.uri in uris }

    override suspend fun songsByIds(ids: List<String>): List<Song> =
        songs.value.filter { it.id in ids }

    override suspend fun refreshLibrary() = Unit

    override suspend fun addFolderSource(folderUri: String) = Unit

    override suspend fun removeFolderSource(folderUri: String) = Unit

    override suspend fun setFavourite(songId: String, isFavourite: Boolean) {
        songs.value =
            songs.value.map { song ->
                if (song.id == songId) song.copy(isFavourite = isFavourite) else song
            }
    }

    override suspend fun upsertStreamStations(stations: List<StreamStation>): List<Song> {
        val existingByUri = songs.value.associateBy(Song::uri)
        val imported = stations.distinctBy(StreamStation::uri).map { station ->
            val existing = existingByUri[station.uri]
            Song(
                id = existing?.id ?: "stream:${station.uri}",
                fileName = station.uri,
                title = station.title,
                artist = station.artist,
                album = "Online streams",
                durationMillis = station.durationSeconds.coerceAtLeast(0) * 1_000,
                dateAddedEpochSeconds = System.currentTimeMillis() / 1_000,
                folderName = "Online streams",
                uri = station.uri,
                mimeType = "audio/*",
                isFavourite = existing?.isFavourite ?: false,
                source = SongSource.STREAM
            )
        }

        songs.value = songs.value.filterNot { song -> song.uri in imported.map(Song::uri).toSet() } + imported
        return imported
    }

    override suspend fun updateStreamMetadata(songId: String, title: String, artist: String) {
        songs.value =
            songs.value.map { song ->
                if (song.id == songId && song.source == SongSource.STREAM) {
                    song.copy(title = title, artist = artist)
                } else {
                    song
                }
            }
    }
}
