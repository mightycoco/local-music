package com.localmusic.player.bluetooth

/** A connected A2DP device available for Car Mode configuration. */
data class CarAudioDevice(
    val id: String,
    val name: String,
    val isLikelyCarDevice: Boolean
)