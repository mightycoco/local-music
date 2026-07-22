package com.localmusic.player.settings

data class SettingsBundle(
    val folderUris: List<String>,
    val carModeEnabled: Boolean,
    val lastSelectedSort: String
)