package com.localmusic.player.domain.model

/** A playable station returned by an online radio directory. */
data class RadioStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?
) {
    fun toStreamStation() =
        StreamStation(
            title = name,
            artist = "Online radio",
            durationSeconds = -1L,
            uri = streamUrl,
            artworkUri = faviconUrl
        )
}