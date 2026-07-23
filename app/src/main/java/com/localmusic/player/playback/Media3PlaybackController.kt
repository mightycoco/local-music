package com.localmusic.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.repository.PlaybackController
import com.localmusic.player.domain.repository.PlaybackSnapshot
import com.localmusic.player.domain.repository.RepeatMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/** Sends local queue playback commands to the app Media3 session service. */
class Media3PlaybackController(
    private val context: Context,
    private val queueFactory: PlaybackQueueFactory = PlaybackQueueFactory()
) : PlaybackController {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun observePlayback(): Flow<PlaybackSnapshot> = callbackFlow {
        var controller: MediaController? = null
        var listener: Player.Listener? = null

        withController { mediaController ->
            controller = mediaController
            fun emitSnapshot() {
                trySend(mediaController.toPlaybackSnapshot())
            }

            listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    emitSnapshot()
                }
            }
            mediaController.addListener(listener!!)
            emitSnapshot()
        }

        val ticker = launch {
            while (true) {
                controller?.let { trySend(it.toPlaybackSnapshot()) }
                delay(500L)
            }
        }

        awaitClose {
            ticker.cancel()
            listener?.let { controller?.removeListener(it) }
        }
    }

    override fun play(songs: List<Song>, startSongId: String) {
        val queue = queueFactory.createQueue(songs, startSongId)
        if (queue.isEmpty()) return

        withController { controller ->
            controller.setMediaItems(queue)
            controller.prepare()
            controller.play()
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
            controller.repeatMode = when (mode) {
                RepeatMode.Off -> Player.REPEAT_MODE_OFF
                RepeatMode.One -> Player.REPEAT_MODE_ONE
                RepeatMode.All -> Player.REPEAT_MODE_ALL
            }
        }
    }

    private fun withController(command: (MediaController) -> Unit) {
        val future = controllerFuture ?: MediaController.Builder(context, sessionToken()).buildAsync().also {
            controllerFuture = it
        }
        future.addListener(
            {
                val controller = future.get()
                command(controller)
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun sessionToken(): SessionToken =
        SessionToken(context, ComponentName(context, LocalMusicPlaybackService::class.java))

    private fun MediaController.toPlaybackSnapshot(): PlaybackSnapshot = PlaybackSnapshot(
        songId = currentMediaItem?.mediaId,
        isPlaying = isPlaying,
        positionMillis = currentPosition.coerceAtLeast(0L),
        durationMillis = duration.takeIf { it > 0L } ?: 0L,
        isShuffleEnabled = shuffleModeEnabled,
        repeatMode = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.One
            Player.REPEAT_MODE_ALL -> RepeatMode.All
            else -> RepeatMode.Off
        }
    )
}