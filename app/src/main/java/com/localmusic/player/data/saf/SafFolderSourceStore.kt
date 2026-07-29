package com.localmusic.player.data.saf

/** Stores user-selected Storage Access Framework folder tree URIs. */
interface SafFolderSourceStore {
    fun folders(): List<String>

    fun add(folderUri: String): Boolean

    fun remove(folderUri: String): Boolean
}
