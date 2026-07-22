package com.localmusic.player.data.saf

import android.content.SharedPreferences

/** SharedPreferences-backed SAF source store for small user-selected folder lists. */
class SharedPreferencesSafFolderSourceStore(
    private val sharedPreferences: SharedPreferences
) : SafFolderSourceStore {
    override fun folders(): List<String> = sharedPreferences
        .getStringSet(KEY_FOLDER_URIS, emptySet())
        .orEmpty()
        .sorted()

    override fun add(folderUri: String): Boolean {
        val updatedFolders = folders().toMutableSet()
        val added = updatedFolders.add(folderUri)
        if (added) {
            sharedPreferences.edit().putStringSet(KEY_FOLDER_URIS, updatedFolders).apply()
        }
        return added
    }

    private companion object {
        const val KEY_FOLDER_URIS = "saf_folder_uris"
    }
}