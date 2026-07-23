package com.localmusic.player.artwork

import android.content.SharedPreferences

/** Persistent user settings controlling optional artwork metadata downloads. */
class ArtworkPreferences(
    private val sharedPreferences: SharedPreferences
) {
    fun isExternalArtworkDownloadEnabled(): Boolean = sharedPreferences.getBoolean(
        KEY_EXTERNAL_ARTWORK_DOWNLOAD_ENABLED,
        true
    )

    fun setExternalArtworkDownloadEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_EXTERNAL_ARTWORK_DOWNLOAD_ENABLED, enabled)
            .apply()
    }

    private companion object {
        const val KEY_EXTERNAL_ARTWORK_DOWNLOAD_ENABLED = "external_artwork_download_enabled"
    }
}