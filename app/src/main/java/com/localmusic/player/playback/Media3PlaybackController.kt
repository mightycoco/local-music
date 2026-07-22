package com.localmusic.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.repository.PlaybackController

/** Sends local queue playback commands to the app Media3 session service. */
class Media3PlaybackController(
    private val context: Context,
    private val queueFactory: PlaybackQueueFactory = PlaybackQueueFactory()
) : PlaybackController {
    override fun play(songs: List<Song>, startSongId: String) {
        val queue = queueFactory.createQueue(songs, startSongId)
        if (queue.isEmpty()) return

        withController { controller ->
            controller.setMediaItems(queue)
            controller.prepare()
            controller.play()
        }
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

    private fun withController(command: (MediaController) -> Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, LocalMusicPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                val controller = controllerFuture.get()
                command(controller)
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}