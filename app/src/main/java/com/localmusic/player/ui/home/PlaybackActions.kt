package com.localmusic.player.ui.home

import androidx.compose.runtime.Immutable

@Immutable
internal data class PlaybackActions(
    val playPause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val seekTo: (Float) -> Unit,
    val toggleShuffle: () -> Unit,
    val cycleRepeatMode: () -> Unit,
    val setVisualizerEnabled: (Boolean) -> Unit
)