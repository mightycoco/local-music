package com.localmusic.player.domain.model

data class RemoteStreamRoute(
    val id: String,
    val name: String
)

enum class RemoteStreamStatus {
    Unavailable,
    Connecting,
    Buffering,
    Playing,
    Paused,
    Error
}

data class RemoteStreamState(
    val status: RemoteStreamStatus = RemoteStreamStatus.Unavailable,
    val route: RemoteStreamRoute? = null,
    val requestedSongId: String? = null,
    val requestedUri: String? = null,
    val errorMessage: String? = null
)
