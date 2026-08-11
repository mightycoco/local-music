package com.localmusic.player.artwork

import android.content.SharedPreferences

/** Persistent user settings controlling artwork discovery and presentation. */
class ArtworkPreferences(private val sharedPreferences: SharedPreferences) {
    fun isExternalArtworkDownloadEnabled(): Boolean =
            sharedPreferences.getBoolean(KEY_EXTERNAL_ARTWORK_DOWNLOAD_ENABLED, false)

    fun setExternalArtworkDownloadEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_EXTERNAL_ARTWORK_DOWNLOAD_ENABLED, enabled).apply()
    }

    fun isVisualizerPreferred(): Boolean =
            sharedPreferences.getBoolean(KEY_VISUALIZER_PREFERRED, false)

    fun setVisualizerPreferred(preferred: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_VISUALIZER_PREFERRED, preferred).apply()
    }

    private companion object {
        const val KEY_EXTERNAL_ARTWORK_DOWNLOAD_ENABLED = "external_artwork_download_enabled"
        const val KEY_VISUALIZER_PREFERRED = "visualizer_preferred"
    }
}
