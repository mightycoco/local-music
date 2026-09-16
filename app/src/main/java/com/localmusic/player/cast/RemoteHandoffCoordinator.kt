package com.localmusic.player.cast

import com.localmusic.player.domain.model.RemoteStreamState
import com.localmusic.player.domain.model.RemoteStreamStatus
import com.localmusic.player.domain.model.Song

internal class RemoteHandoffCoordinator {
    private var pausedRequest: Pair<String?, String?>? = null

    fun shouldPauseLocal(state: RemoteStreamState, currentSong: Song?): Boolean {
        if (state.status == RemoteStreamStatus.Unavailable) {
            pausedRequest = null
            return false
        }
        val request = state.requestedSongId to state.requestedUri
        val matches =
            currentSong != null &&
                    state.requestedSongId == currentSong.id &&
                    state.requestedUri == currentSong.uri
        return state.status == RemoteStreamStatus.Playing && matches && pausedRequest != request
    }

    fun markLocalPaused(state: RemoteStreamState) {
        pausedRequest = state.requestedSongId to state.requestedUri
    }
}