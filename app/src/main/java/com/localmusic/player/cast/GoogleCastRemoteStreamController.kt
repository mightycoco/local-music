package com.localmusic.player.cast

import android.content.Context
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import com.localmusic.player.domain.model.RemoteStreamRoute
import com.localmusic.player.domain.model.RemoteStreamState
import com.localmusic.player.domain.model.RemoteStreamStatus
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SongSource
import com.localmusic.player.domain.repository.RemoteStreamController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoogleCastRemoteStreamController(context: Context) : RemoteStreamController {
    private val castContext = CastContext.getSharedInstance(context.applicationContext)
    private val sessionManager = castContext.sessionManager
    private val mutableState = MutableStateFlow(RemoteStreamState())
    private var preparedSong: Song? = null
    private var activeClient: RemoteMediaClient? = null
    private var activeRequest: RemoteRequest? = null
    private var requestGeneration = 0L

    override val state: StateFlow<RemoteStreamState> = mutableState.asStateFlow()

    private val mediaClientCallback =
        object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                updateFromMediaStatus()
            }

            override fun onMetadataUpdated() {
                updateFromMediaStatus()
            }
        }

    private val sessionListener =
        object : SessionManagerListener<CastSession> {
            override fun onSessionStarting(session: CastSession) {
                updateConnecting(session)
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                attachSession(session)
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                failCurrentRequest("Could not connect to Cast device")
            }

            override fun onSessionEnding(session: CastSession) = Unit

            override fun onSessionEnded(session: CastSession, error: Int) {
                detachClient()
                mutableState.value = RemoteStreamState()
            }

            override fun onSessionResuming(session: CastSession, sessionId: String) {
                updateConnecting(session)
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                attachSession(session)
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                failCurrentRequest("Could not reconnect to Cast device")
            }

            override fun onSessionSuspended(session: CastSession, reason: Int) {
                detachClient()
                mutableState.value = RemoteStreamState()
            }
        }

    init {
        sessionManager.addSessionManagerListener(sessionListener, CastSession::class.java)
        sessionManager.currentCastSession?.takeIf(CastSession::isConnected)?.let(::attachSession)
    }

    override fun prepare(song: Song?) {
        val eligibleSong = song?.takeIf(::isCastEligible)
        if (preparedSong?.id == eligibleSong?.id && preparedSong?.uri == eligibleSong?.uri) return

        preparedSong = eligibleSong
        activeRequest = null
        if (eligibleSong == null) {
            mutableState.value = RemoteStreamState()
            return
        }
        sessionManager.currentCastSession?.takeIf(CastSession::isConnected)?.let(::loadPreparedSong)
    }

    override fun play() {
        activeClient?.play()
    }

    override fun pause() {
        activeClient?.pause()
    }

    override fun stopAndDisconnect() {
        activeClient?.stop()
        sessionManager.endCurrentSession(true)
        activeRequest = null
        mutableState.value = RemoteStreamState()
    }

    override fun close() {
        detachClient()
        sessionManager.removeSessionManagerListener(sessionListener, CastSession::class.java)
    }

    private fun attachSession(session: CastSession) {
        detachClient()
        activeClient = session.remoteMediaClient?.also { it.registerCallback(mediaClientCallback) }
        loadPreparedSong(session)
    }

    private fun loadPreparedSong(session: CastSession) {
        val song = preparedSong ?: return
        val client = session.remoteMediaClient ?: return
        if (activeClient !== client) {
            detachClient()
            activeClient = client.also { it.registerCallback(mediaClientCallback) }
        }

        val request = RemoteRequest(++requestGeneration, song.id, song.uri)
        activeRequest = request
        mutableState.value = request.toState(RemoteStreamStatus.Connecting, session.route())
        client.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(song.toMediaInfo())
                .setAutoplay(true)
                .build()
        ).setResultCallback { result ->
            if (activeRequest != request) return@setResultCallback
            if (!result.status.isSuccess) {
                failCurrentRequest(result.status.statusMessage ?: "Cast device rejected the stream")
            }
        }
    }

    private fun updateFromMediaStatus() {
        val request = activeRequest ?: return
        val client = activeClient ?: return
        if (client.mediaInfo?.contentId != request.uri) return

        val status =
            when (client.playerState) {
                MediaStatus.PLAYER_STATE_BUFFERING -> RemoteStreamStatus.Buffering
                MediaStatus.PLAYER_STATE_PLAYING -> RemoteStreamStatus.Playing
                MediaStatus.PLAYER_STATE_PAUSED -> RemoteStreamStatus.Paused
                MediaStatus.PLAYER_STATE_IDLE -> {
                    if (client.idleReason == MediaStatus.IDLE_REASON_ERROR) {
                        failCurrentRequest("Cast device could not play the stream")
                    }
                    return
                }
                else -> return
            }
        mutableState.value = request.toState(status, sessionManager.currentCastSession?.route())
    }

    private fun updateConnecting(session: CastSession) {
        val song = preparedSong ?: return
        mutableState.value =
            RemoteStreamState(
                status = RemoteStreamStatus.Connecting,
                route = session.route(),
                requestedSongId = song.id,
                requestedUri = song.uri
            )
    }

    private fun failCurrentRequest(message: String) {
        val request = activeRequest
        mutableState.value =
            RemoteStreamState(
                status = RemoteStreamStatus.Error,
                route = sessionManager.currentCastSession?.route(),
                requestedSongId = request?.songId,
                requestedUri = request?.uri,
                errorMessage = message
            )
    }

    private fun detachClient() {
        activeClient?.unregisterCallback(mediaClientCallback)
        activeClient = null
    }
}

private data class RemoteRequest(val generation: Long, val songId: String, val uri: String) {
    fun toState(status: RemoteStreamStatus, route: RemoteStreamRoute?) =
        RemoteStreamState(
            status = status,
            route = route,
            requestedSongId = songId,
            requestedUri = uri
        )
}

private fun CastSession.route(): RemoteStreamRoute? =
    castDevice?.let { device ->
        RemoteStreamRoute(id = device.deviceId, name = device.friendlyName ?: "Cast device")
    }

private fun isCastEligible(song: Song): Boolean =
    song.source == SongSource.STREAM && Uri.parse(song.uri).scheme?.lowercase() in setOf("http", "https")

private fun Song.toMediaInfo(): MediaInfo {
    val metadata =
        MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, title)
            putString(MediaMetadata.KEY_ARTIST, artist)
            artworkUri?.let { artwork -> addImage(WebImage(Uri.parse(artwork))) }
        }
    return MediaInfo.Builder(uri)
        .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
        .setContentType(mimeType.ifBlank { "audio/mpeg" })
        .setMetadata(metadata)
        .build()
}