package com.localmusic.player.data.saf

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.localmusic.player.data.mediastore.MusicScanResult
import com.localmusic.player.data.mediastore.MusicScanner
import com.localmusic.player.domain.model.Song
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Recursively scans user-selected SAF folders for supported local audio files. */
class SafFolderMusicScanner(
        private val context: Context,
        private val sourceStore: SafFolderSourceStore,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MusicScanner {
    override suspend fun scan(): MusicScanResult =
            withContext(ioDispatcher) {
                val songs = mutableListOf<Song>()
                var isComplete = true
                sourceStore.folders().forEach { folderUri ->
                    val root = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
                    if (root == null || !root.exists() || !root.canRead()) {
                        isComplete = false
                    } else {
                        root.walkAudioFiles(songs)
                    }
                }
                MusicScanResult(songs, isComplete)
            }

    private fun DocumentFile.walkAudioFiles(songs: MutableList<Song>) {
        val pending = ArrayDeque<DocumentFile>()
        pending.add(this)
        while (pending.isNotEmpty()) {
            val document = pending.removeLast()
            if (document.isFile) {
                document.toSongOrNull()?.let(songs::add)
            } else if (document.isDirectory) {
                pending.addAll(document.listFiles())
            }
        }
    }

    private fun DocumentFile.toSongOrNull(): Song? {
        val mimeType = type.orEmpty()
        val fileName = name.orEmpty()
        if (!mimeType.isSupportedAudioMimeType() && !fileName.hasSupportedAudioExtension())
                return null

        val title = fileName.substringBeforeLast('.', fileName).ifBlank { fileName }
        val dateAdded = (lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()) / 1_000L

        return Song(
                id = "saf:${uri}",
                fileName = fileName,
                title = title,
                artist = "Unknown Artist",
                album = "Unknown Album",
                genre = "Unknown Genre",
                durationMillis = 0L,
                dateAddedEpochSeconds = dateAdded,
                folderName = uri.path.orEmpty().substringBeforeLast('/').substringAfterLast('/'),
                uri = uri.toString(),
                mimeType = mimeType,
                sizeBytes = length()
        )
    }

    private fun String.isSupportedAudioMimeType(): Boolean = lowercase() in supportedMimeTypes

    private fun String.hasSupportedAudioExtension(): Boolean =
            supportedAudioExtensions.any { extension -> endsWith(extension, ignoreCase = true) }

    private companion object {
        val supportedMimeTypes =
                setOf(
                        "audio/mpeg",
                        "audio/mp4",
                        "audio/aac",
                        "audio/flac",
                        "audio/wav",
                        "audio/x-wav",
                        "audio/ogg",
                        "audio/opus",
                        "audio/aiff",
                        "audio/x-aiff"
                )
        val supportedAudioExtensions =
                setOf(".mp3", ".m4a", ".aac", ".flac", ".wav", ".ogg", ".opus", ".aiff", ".aif")
    }
}
