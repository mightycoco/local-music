package com.localmusic.player.playlist

import com.localmusic.player.data.database.PlaylistDao
import com.localmusic.player.data.database.PlaylistEntity

class RoomPlaylistStore(
        private val playlistDao: PlaylistDao,
        private val legacyStore: PlaylistStore? = null,
        private val codec: M3uPlaylistCodec = M3uPlaylistCodec()
) : PlaylistStore {
    override suspend fun playlists(): List<M3uPlaylist> {
        var entities = playlistDao.all()
        if (entities.isEmpty()) {
            for (playlist in legacyStore?.playlists().orEmpty()) {
                save(playlist)
            }
            entities = playlistDao.all()
        }
        return entities
                .mapNotNull { entity ->
                    runCatching { codec.parse(entity.name, entity.content) }.getOrNull()
                }
                .withQueueFirst()
    }

    override suspend fun save(playlist: M3uPlaylist) {
        playlistDao.upsert(PlaylistEntity(playlist.name, codec.export(playlist)))
    }

    override suspend fun delete(name: String) {
        playlistDao.delete(name)
    }
}
