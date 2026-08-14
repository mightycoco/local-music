package com.localmusic.player.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
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
    private val _previewError = MutableStateFlow<String?>(null)
    override val previewError = _previewError.asStateFlow()

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _previewError.value = formatPlaybackError(error)
                    stop()
                }
            }
        )
    }

    override fun play(station: RadioStation) {
        _previewError.value = null
        _previewStation.value = station
        try {
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
        } catch (error: Throwable) {
            _previewError.value = formatPlaybackError(error)
            stop()
        }
    }

    override fun stop() {
        player.stop()
        player.clearMediaItems()
        _previewStation.value = null
    }

    override fun close() {
        player.release()
        _previewStation.value = null
        _previewError.value = null
    }

    private fun formatPlaybackError(error: Throwable): String {
        val decoderError = generateSequence(error) { it.cause }
            .filterIsInstance<MediaCodecRenderer.DecoderInitializationException>()
            .firstOrNull()
        if (decoderError != null) {
            val decoderName = decoderError.message
                ?.substringAfter("decoder ", "")
                ?.substringBefore(" ")
                ?.takeIf(String::isNotBlank)
                ?: "unknown"
            return "Unsupported codec \"$decoderName\""
        }

        val message = generateSequence(error) { it.cause }
            .mapNotNull { it.message?.takeIf(String::isNotBlank) }
            .firstOrNull()
            ?: "Unable to play this stream"
        return if (message.contains("HlsMediaSource", ignoreCase = true) ||
            message.contains("exoplayer.hls", ignoreCase = true)
        ) {
            "Unsupported stream format \"HLS\""
        } else {
            message
        }
    }
}