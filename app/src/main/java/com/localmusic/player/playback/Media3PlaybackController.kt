package com.localmusic.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.repository.PlaybackController
import com.localmusic.player.domain.repository.PlaybackSnapshot
import com.localmusic.player.domain.repository.RepeatMode
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Sends local queue playback commands to the app Media3 session service. */
@UnstableApi
class Media3PlaybackController(
    private val context: Context,
    private val queueFactory: PlaybackQueueFactory = PlaybackQueueFactory()
) : PlaybackController {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun observePlayback(): Flow<PlaybackSnapshot> =
        callbackFlow {
            var controller: MediaController? = null
            var listener: Player.Listener? = null
            val visualizerLevels = AtomicReference<List<Float>>(emptyList())
            val queueSongIds = AtomicReference<List<String>>(emptyList())

            fun emitSnapshot() {
                controller?.let { mediaController ->
                    trySend(
                        mediaController.toPlaybackSnapshot(
                            queueSongIds.get(),
                            visualizerLevels.get()
                        )
                    )
                }
            }

            withController { mediaController ->
                controller = mediaController

                listener =
                    object : Player.Listener {
                        override fun onEvents(
                            player: Player,
                            events: Player.Events
                        ) {
                            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                                queueSongIds.set(player.queueSongIds())
                            }
                            emitSnapshot()
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            emitSnapshot()
                        }
                    }
                mediaController.addListener(listener!!)
                queueSongIds.set(mediaController.queueSongIds())
                emitSnapshot()
            }

            val visualizerCollector = launch {
                PlaybackAudioProcessor.levels.collect { levels ->
                    visualizerLevels.set(levels)
                    emitSnapshot()
                }
            }

            val ticker = launch {
                while (true) {
                    emitSnapshot()
                    delay(500L)
                }
            }

            awaitClose {
                ticker.cancel()
                visualizerCollector.cancel()
                listener?.let { controller?.removeListener(it) }
            }
        }
            .buffer(Channel.CONFLATED)

    override fun play(songs: List<Song>, startSongId: String) {
        val queue = queueFactory.createQueue(songs)
        if (queue.isEmpty()) return
        val startIndex = songs.indexOfFirst { it.id == startSongId }.coerceAtLeast(0)

        withController { controller ->
            controller.setMediaItems(queue, startIndex, 0L)
            controller.prepare()
            controller.play()
        }
    }

    override fun restoreQueue(songs: List<Song>, startSongId: String) {
        val queue = queueFactory.createQueue(songs)
        if (queue.isEmpty()) return
        val startIndex = songs.indexOfFirst { it.id == startSongId }.coerceAtLeast(0)

        withController { controller ->
            controller.setMediaItems(queue, startIndex, 0L)
            controller.prepare()
            controller.pause()
        }
    }

    override fun synchronizeQueue(songs: List<Song>, currentSongId: String?) {
        val queue = queueFactory.createQueue(songs)
        withController { controller ->
            if (queue.isEmpty()) {
                controller.clearMediaItems()
                return@withController
            }
            val retainedSongId = currentSongId?.takeIf { id -> songs.any { it.id == id } }
            val startIndex = songs.indexOfFirst { it.id == retainedSongId }.coerceAtLeast(0)
            val startPosition =
                if (controller.currentMediaItem?.mediaId == retainedSongId) {
                    controller.currentPosition.coerceAtLeast(0L)
                } else {
                    0L
                }
            val shouldPlay = controller.playWhenReady
            controller.setMediaItems(queue, startIndex, startPosition)
            controller.prepare()
            controller.playWhenReady = shouldPlay
        }
    }

    override fun enqueue(song: Song) {
        val item = queueFactory.createQueue(listOf(song)).singleOrNull() ?: return
        withController { controller -> controller.addMediaItem(item) }
    }

    override fun clearQueue() {
        withController { controller -> controller.clearMediaItems() }
    }

    override fun resume() {
        withController { controller -> controller.play() }
    }

    override fun pause() {
        withController { controller -> controller.pause() }
    }

    override fun skipToNext() {
        withController { controller -> controller.seekToNextMediaItem() }
    }

    override fun skipToPrevious() {
        withController { controller -> controller.seekToPreviousMediaItem() }
    }

    override fun seekTo(progress: Float) {
        withController { controller ->
            val duration = controller.duration.takeIf { it > 0 } ?: return@withController
            controller.seekTo((duration * progress.coerceIn(0f, 1f)).toLong())
        }
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        withController { controller -> controller.shuffleModeEnabled = enabled }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        withController { controller ->
            controller.repeatMode =
                when (mode) {
                    RepeatMode.Off -> Player.REPEAT_MODE_OFF
                    RepeatMode.One -> Player.REPEAT_MODE_ONE
                    RepeatMode.All -> Player.REPEAT_MODE_ALL
                }
        }
    }

    override fun setVisualizerEnabled(enabled: Boolean) {
        PlaybackAudioProcessor.setEnabled(enabled)
    }

    override fun close() {
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
    }

    private fun withController(
        canRetry: Boolean = true,
        command: (MediaController) -> Unit
    ) {
        val existingFuture = controllerFuture
        val future =
            if (existingFuture == null || existingFuture.isCancelled ||
                (existingFuture.isDone && runCatching(existingFuture::get).getOrNull()?.isConnected != true)
            ) {
                existingFuture?.let(MediaController::releaseFuture)
                MediaController.Builder(context, sessionToken()).buildAsync().also {
                    controllerFuture = it
                }
            } else {
                existingFuture
            }
        future.addListener(
            {
                val controller = runCatching(future::get).getOrNull()
                if (controller?.isConnected == true) {
                    val commandResult = runCatching { command(controller) }
                    if (commandResult.isFailure && controllerFuture === future) {
                        controllerFuture = null
                        MediaController.releaseFuture(future)
                        if (canRetry) withController(canRetry = false, command = command)
                    }
                } else if (controllerFuture === future) {
                    controllerFuture = null
                    MediaController.releaseFuture(future)
                    if (canRetry) withController(canRetry = false, command = command)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun sessionToken(): SessionToken =
        SessionToken(context, ComponentName(context, LocalMusicPlaybackService::class.java))

    private fun MediaController.toPlaybackSnapshot(
        queueSongIds: List<String>,
        visualizerLevels: List<Float>
    ): PlaybackSnapshot =
        PlaybackSnapshot(
            songId = currentMediaItem?.mediaId,
            queueSongIds = queueSongIds,
            title = mediaMetadata.title?.toString(),
            artist = mediaMetadata.artist?.toString(),
            isPlaying = isPlaying,
            positionMillis = currentPosition.coerceAtLeast(0L),
            durationMillis = duration.takeIf { it > 0L } ?: 0L,
            isShuffleEnabled = shuffleModeEnabled,
            repeatMode =
                when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.One
                    Player.REPEAT_MODE_ALL -> RepeatMode.All
                    else -> RepeatMode.Off
                },
            visualizerLevels = visualizerLevels,
            errorMessage = playerError?.cause?.message ?: playerError?.message
        )

    private fun Player.queueSongIds(): List<String> =
        List(mediaItemCount) { index -> getMediaItemAt(index).mediaId }
}
