package com.localmusic.player.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.localmusic.player.domain.model.RadioStation
import com.localmusic.player.domain.repository.RadioPreviewController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Dedicated one-item player for auditioning a Radio Browser result. */
@UnstableApi
class Media3RadioPreviewController(context: Context) : RadioPreviewController {
    private val player = ExoPlayer.Builder(context).build()
    private val _previewStation = MutableStateFlow<RadioStation?>(null)
    override val previewStation = _previewStation.asStateFlow()

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    stop()
                }
            }
        )
    }

    override fun play(station: RadioStation) {
        _previewStation.value = station
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(station.streamUrl.toUri())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(station.name)
                        .setArtist("Online radio")
                        .build()
                )
                .build()
        )
        player.prepare()
        player.play()
    }

    override fun stop() {
        player.stop()
        player.clearMediaItems()
        _previewStation.value = null
    }

    override fun close() {
        player.release()
        _previewStation.value = null
    }
}