package com.localmusic.player.domain.repository

import com.localmusic.player.domain.model.RadioStation

/** Online directory boundary; UI and ViewModels never perform station lookups directly. */
interface RadioStationDirectory {
    suspend fun search(query: String): List<RadioStation>
}