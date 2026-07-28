package com.localmusic.player.playback

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Media3 session service for local playback and external media controls. */
class LocalMusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var visualizerRetry: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().also { this.player = it }
        player.addListener(
                object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        PlaybackVisualizer.configure(this@LocalMusicPlaybackService, audioSessionId)
                    }
                }
        )
        mediaSession = MediaSession.Builder(this, player).build()
        visualizerRetry =
                object : Runnable {
                            override fun run() {
                                PlaybackVisualizer.configure(
                                        this@LocalMusicPlaybackService,
                                        player.audioSessionId
                                )
                                android.os.Handler(mainLooper).postDelayed(this, 1_000L)
                            }
                        }
                        .also { android.os.Handler(mainLooper).post(it) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
            mediaSession

    override fun onDestroy() {
        visualizerRetry?.let { android.os.Handler(mainLooper).removeCallbacks(it) }
        visualizerRetry = null
        PlaybackVisualizer.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}

internal object PlaybackVisualizer {
    private val _levels = MutableStateFlow<List<Float>>(emptyList())
    val levels = _levels.asStateFlow()

    private var visualizer: Visualizer? = null
    private var audioSessionId = -1

    fun configure(context: Context, sessionId: Int) {
        if (sessionId <= 0 ||
                        ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED ||
                        audioSessionId == sessionId && visualizer != null
        ) {
            return
        }
        release()
        visualizer =
                runCatching {
                            Visualizer(sessionId).apply {
                                captureSize = Visualizer.getCaptureSizeRange().last()
                                setDataCaptureListener(
                                        object : Visualizer.OnDataCaptureListener {
                                            override fun onWaveFormDataCapture(
                                                    visualizer: Visualizer,
                                                    waveform: ByteArray,
                                                    samplingRate: Int
                                            ) {
                                                _levels.value = waveform.toVisualizerLevels()
                                            }

                                            override fun onFftDataCapture(
                                                    visualizer: Visualizer,
                                                    fft: ByteArray,
                                                    samplingRate: Int
                                            ) = Unit
                                        },
                                        Visualizer.getMaxCaptureRate() / 2,
                                        true,
                                        false
                                )
                                enabled = true
                            }
                        }
                        .getOrNull()
        audioSessionId = if (visualizer == null) -1 else sessionId
    }

    fun release() {
        visualizer?.release()
        visualizer = null
        audioSessionId = -1
        _levels.value = emptyList()
    }

    private fun ByteArray.toVisualizerLevels(barCount: Int = 9): List<Float> =
            List(barCount) { barIndex ->
                val start = barIndex * size / barCount
                val end = ((barIndex + 1) * size / barCount).coerceAtMost(size)
                val peak =
                        slice(start until end).maxOfOrNull { sample ->
                            kotlin.math.abs((sample.toInt() and 0xFF) - 128)
                        }
                                ?: 0
                (peak / 128f).coerceIn(0f, 1f)
            }
}
