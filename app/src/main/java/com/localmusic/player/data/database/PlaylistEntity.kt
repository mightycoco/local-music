package com.localmusic.player.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(@PrimaryKey val name: String, val content: String)
