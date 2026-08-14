package com.localmusic.player.domain.repository

import com.localmusic.player.domain.model.RadioStation
import kotlinx.coroutines.flow.StateFlow

/** Controls the short-lived radio preview without changing the main playback queue. */
interface RadioPreviewController {
    val previewStation: StateFlow<RadioStation?>
    val previewError: StateFlow<String?>

    fun play(station: RadioStation)
    fun stop()
    fun close()
}