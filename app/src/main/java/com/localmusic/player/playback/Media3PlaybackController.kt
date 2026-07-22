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

        val sessionToken = SessionToken(context, ComponentName(context, LocalMusicPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                val controller = controllerFuture.get()
                controller.setMediaItems(queue)
                controller.prepare()
                controller.play()
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}