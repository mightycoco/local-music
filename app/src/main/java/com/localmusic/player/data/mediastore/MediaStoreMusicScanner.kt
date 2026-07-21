package com.localmusic.player.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.localmusic.player.domain.model.Song
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Scans Android MediaStore for supported local audio files. */
class MediaStoreMusicScanner(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MusicScanner {
    override suspend fun scan(): List<Song> = withContext(ioDispatcher) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            }
        }.toTypedArray()

        val songs = mutableListOf<Song>()
        contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val mimeType = cursor.string(MediaStore.MediaColumns.MIME_TYPE)
                if (mimeType !in supportedMimeTypes) continue

                val id = cursor.long(MediaStore.Audio.Media._ID)
                val fileName = cursor.string(MediaStore.MediaColumns.DISPLAY_NAME).ifBlank { "audio-$id" }
                val title = cursor.string(MediaStore.Audio.Media.TITLE).ifBlank {
                    fileName.substringBeforeLast('.', fileName)
                }

                songs += Song(
                    id = "mediastore:$id",
                    fileName = fileName,
                    title = title,
                    artist = cursor.string(MediaStore.Audio.Media.ARTIST).ifBlank { "Unknown Artist" },
                    album = cursor.string(MediaStore.Audio.Media.ALBUM).ifBlank { "Unknown Album" },
                    durationMillis = cursor.long(MediaStore.Audio.Media.DURATION),
                    dateAddedEpochSeconds = cursor.long(MediaStore.Audio.Media.DATE_ADDED),
                    folderName = cursor.folderName(),
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    mimeType = mimeType,
                    sizeBytes = cursor.long(MediaStore.MediaColumns.SIZE)
                )
            }
        }

        songs
    }

    private fun Cursor.folderName(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "Device Music"
        return string(MediaStore.MediaColumns.RELATIVE_PATH)
            .trim('/', '\\')
            .substringAfterLast('/', "Device Music")
            .ifBlank { "Device Music" }
    }

    private fun Cursor.string(column: String): String {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index).orEmpty() else ""
    }

    private fun Cursor.long(column: String): Long {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getLong(index) else 0L
    }

    private companion object {
        val supportedMimeTypes = setOf(
            "audio/mpeg",
            "audio/aac",
            "audio/mp4",
            "audio/x-m4a",
            "audio/flac",
            "audio/wav",
            "audio/x-wav",
            "audio/aiff",
            "audio/x-aiff",
            "audio/ogg",
            "audio/opus"
        )
    }
}