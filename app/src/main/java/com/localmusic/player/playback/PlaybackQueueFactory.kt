package com.localmusic.player.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.localmusic.player.domain.model.Song

/** Converts library songs into a Media3 queue without exposing Media3 to UI state. */
class PlaybackQueueFactory {
    fun createQueue(songs: List<Song>): List<MediaItem> {
        return songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(song.uri.toUri())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }
    }
}