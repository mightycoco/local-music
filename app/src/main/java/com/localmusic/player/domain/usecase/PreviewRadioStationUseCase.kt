package com.localmusic.player.domain.usecase

import com.localmusic.player.domain.model.RadioStation
import com.localmusic.player.domain.repository.RadioPreviewController

class PreviewRadioStationUseCase(private val previewController: RadioPreviewController) {
    val previewStation = previewController.previewStation

    fun play(station: RadioStation) = previewController.play(station)

    fun stop() = previewController.stop()

    fun close() = previewController.close()
}