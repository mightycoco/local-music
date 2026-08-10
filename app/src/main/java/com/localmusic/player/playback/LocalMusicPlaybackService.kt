package com.localmusic.player.playback

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.localmusic.player.ui.home.VISUALIZER_FPS
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Media3 session service for local playback and external media controls. */
class LocalMusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val renderersFactory =
                object : DefaultRenderersFactory(this) {
                    override fun buildAudioSink(
                            context: android.content.Context,
                            enableFloatOutput: Boolean,
                            enableAudioTrackPlaybackParams: Boolean
                    ): AudioSink =
                            DefaultAudioSink.Builder(context)
                                    .setAudioProcessors(arrayOf(PlaybackAudioProcessor))
                                    .build()
                }
        val player = ExoPlayer.Builder(this, renderersFactory).build().also { this.player = it }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
            mediaSession

    override fun onDestroy() {
        PlaybackAudioProcessor.setEnabled(false)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}

internal object PlaybackAudioProcessor : BaseAudioProcessor() {
    private const val LEVELS_UPDATE_INTERVAL_MILLIS = 1_000L / VISUALIZER_FPS
    private const val MINIMUM_VISUALIZER_DECIBELS = -60f
    private const val MINIMUM_VISUALIZER_AMPLITUDE = 0.001f
    private const val VISUALIZER_RESPONSE_EXPONENT = 2f

    private val _levels = MutableStateFlow<List<Float>>(emptyList())
    val levels = _levels.asStateFlow()
    @Volatile private var isEnabled = false
    @Volatile private var lastLevelsUpdateMillis = 0L
    private var inputEncoding = C.ENCODING_INVALID

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            _levels.value = emptyList()
            lastLevelsUpdateMillis = 0L
        }
    }

    override fun onConfigure(
            inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {
        inputEncoding = inputAudioFormat.encoding
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val now = SystemClock.elapsedRealtime()
        if (isEnabled &&
                        inputBuffer.hasRemaining() &&
                        now - lastLevelsUpdateMillis >= LEVELS_UPDATE_INTERVAL_MILLIS
        ) {
            _levels.value = inputBuffer.toVisualizerLevels(inputEncoding)
            lastLevelsUpdateMillis = now
        }
        val inputCopy = inputBuffer.duplicate()
        replaceOutputBuffer(inputCopy.remaining()).put(inputCopy).flip()
        inputBuffer.position(inputBuffer.limit())
    }

    private fun ByteBuffer.toVisualizerLevels(encoding: Int, barCount: Int = 9): List<Float> {
        return when (encoding) {
            C.ENCODING_PCM_8BIT -> toPcm8VisualizerLevels(barCount)
            C.ENCODING_PCM_16BIT -> toPcm16VisualizerLevels(barCount)
            C.ENCODING_PCM_24BIT -> toPcm24VisualizerLevels(barCount)
            C.ENCODING_PCM_32BIT -> toPcm32VisualizerLevels(barCount)
            C.ENCODING_PCM_FLOAT -> toFloatVisualizerLevels(barCount)
            else -> emptyList()
        }
    }

    private fun ByteBuffer.toPcm8VisualizerLevels(barCount: Int): List<Float> =
            toPeakLevels(barCount) { sample -> kotlin.math.abs(sample.toInt() - 128) / 127f }

    private fun ByteBuffer.toPcm16VisualizerLevels(barCount: Int): List<Float> {
        val samples = duplicate().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        if (!samples.hasRemaining()) return emptyList()
        val samplesPerBar = (samples.remaining() / barCount).coerceAtLeast(1)
        return List(barCount) {
            var peak = 0
            repeat(samplesPerBar) {
                if (samples.hasRemaining()) {
                    peak = maxOf(peak, kotlin.math.abs(samples.get().toInt()))
                }
            }
            (peak / Short.MAX_VALUE.toFloat()).toLogarithmicVisualizerLevel()
        }
    }

    private fun ByteBuffer.toFloatVisualizerLevels(barCount: Int): List<Float> {
        val samples = duplicate().order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        if (!samples.hasRemaining()) return emptyList()
        val samplesPerBar = (samples.remaining() / barCount).coerceAtLeast(1)
        return List(barCount) {
            var peak = 0f
            repeat(samplesPerBar) {
                if (samples.hasRemaining()) {
                    peak = maxOf(peak, kotlin.math.abs(samples.get()))
                }
            }
            peak.toLogarithmicVisualizerLevel()
        }
    }

    private fun ByteBuffer.toPcm24VisualizerLevels(barCount: Int): List<Float> {
        val bytes = duplicate().order(ByteOrder.LITTLE_ENDIAN)
        if (bytes.remaining() < 3) return emptyList()
        val samplesPerBar = (bytes.remaining() / 3 / barCount).coerceAtLeast(1)
        return List(barCount) {
            var peak = 0f
            repeat(samplesPerBar) {
                if (bytes.remaining() >= 3) {
                    val sample =
                            (bytes.get().toInt() and 0xFF) or
                                    ((bytes.get().toInt() and 0xFF) shl 8) or
                                    (bytes.get().toInt() shl 16)
                    peak = maxOf(peak, kotlin.math.abs(sample) / 8_388_607f)
                }
            }
            peak.toLogarithmicVisualizerLevel()
        }
    }

    private fun ByteBuffer.toPcm32VisualizerLevels(barCount: Int): List<Float> {
        val samples = duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
        if (!samples.hasRemaining()) return emptyList()
        val samplesPerBar = (samples.remaining() / barCount).coerceAtLeast(1)
        return List(barCount) {
            var peak = 0f
            repeat(samplesPerBar) {
                if (samples.hasRemaining()) {
                    peak = maxOf(peak, kotlin.math.abs(samples.get() / Int.MAX_VALUE.toFloat()))
                }
            }
            peak.toLogarithmicVisualizerLevel()
        }
    }

    private fun ByteBuffer.toPeakLevels(
            barCount: Int,
            bytesPerSample: Int = 1,
            sampleMagnitude: (Byte) -> Float
    ): List<Float> {
        if (remaining() < bytesPerSample) return emptyList()
        val samplesPerBar = (remaining() / bytesPerSample / barCount).coerceAtLeast(1)
        return List(barCount) {
            var peak = 0f
            repeat(samplesPerBar) {
                if (remaining() >= bytesPerSample) {
                    peak = maxOf(peak, sampleMagnitude(get()))
                    repeat(bytesPerSample - 1) { get() }
                }
            }
            peak.toLogarithmicVisualizerLevel()
        }
    }

    private fun Float.toLogarithmicVisualizerLevel(): Float {
        val amplitude = coerceIn(MINIMUM_VISUALIZER_AMPLITUDE, 1f)
        val decibels = 20f * kotlin.math.log10(amplitude)
        val normalizedLevel =
                ((decibels - MINIMUM_VISUALIZER_DECIBELS) / -MINIMUM_VISUALIZER_DECIBELS).coerceIn(
                        0f,
                        1f
                )
        return normalizedLevel.pow(VISUALIZER_RESPONSE_EXPONENT)
    }
}
