package com.localmusic.player.domain.repository

import com.localmusic.player.domain.model.RemoteStreamState
import com.localmusic.player.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

interface RemoteStreamController : AutoCloseable {
    val state: StateFlow<RemoteStreamState>

    fun prepare(song: Song?)

    fun play()

    fun pause()

    fun stopAndDisconnect()

    override fun close()
}