package com.localmusic.player.data.repository

import com.localmusic.player.data.database.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomMusicRepositoryStateMergeTest {
    @Test
    fun mergeRetainedStateKeepsFavouriteAndPlaybackHistory() {
        val scanned = songEntity(
            id = "song-1",
            title = "Updated Title",
            isFavourite = false,
            playCount = 0,
            lastPlayedEpochMillis = null
        )
        val retained = songEntity(
            id = "song-1",
            title = "Old Title",
            isFavourite = true,
            playCount = 7,
            lastPlayedEpochMillis = 1234L
        )

        val merged = listOf(scanned).mergeRetainedState(listOf(retained))

        assertEquals("Updated Title", merged.first().title)
        assertEquals(true, merged.first().isFavourite)
        assertEquals(7, merged.first().playCount)
        assertEquals(1234L, merged.first().lastPlayedEpochMillis)
    }

    @Test
    fun mergeRetainedStateLeavesNewSongsUnchanged() {
        val scanned = songEntity(id = "song-1", isFavourite = false)

        val merged = listOf(scanned).mergeRetainedState(emptyList())

        assertEquals(scanned, merged.first())
    }

    @Test
    fun streamUpsertKeepsThePersistedFavouriteState() {
        val imported = songEntity(id = "stream-new", isFavourite = false)
        val persisted = songEntity(id = "stream-old", isFavourite = true)

        val merged = imported.mergeStreamState(persisted)

        assertEquals("stream-old", merged.id)
        assertEquals(true, merged.isFavourite)
    }

    private fun songEntity(
        id: String,
        title: String = "Song",
        isFavourite: Boolean = false,
        playCount: Int = 0,
        lastPlayedEpochMillis: Long? = null
    ): SongEntity = SongEntity(
        id = id,
        fileName = "$title.flac",
        title = title,
        artist = "Artist",
        album = "Album",
        genre = "Rock",
        albumArtist = "Album Artist",
        composer = "Composer",
        year = 2026,
        comments = "Comment",
        description = "Description",
        durationMillis = 180_000,
        dateAddedEpochSeconds = 1_700_000_000,
        folderName = "Music",
        uri = "content://media/$id",
        mimeType = "audio/flac",
        sizeBytes = 1024,
        playCount = playCount,
        lastPlayedEpochMillis = lastPlayedEpochMillis,
        isFavourite = isFavourite
    )
}

private fun SongEntity.mergeStreamState(existing: SongEntity): SongEntity =
    copy(
        id = existing.id,
        playCount = existing.playCount,
        lastPlayedEpochMillis = existing.lastPlayedEpochMillis,
        isFavourite = existing.isFavourite,
        scanGeneration = existing.scanGeneration
    )
