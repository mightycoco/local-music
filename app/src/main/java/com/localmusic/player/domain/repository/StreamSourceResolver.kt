package com.localmusic.player.domain.repository

import com.localmusic.player.domain.model.StreamStation

/** Resolves a user-entered HTTP(S) stream or remote M3U source into playable stations. */
interface StreamSourceResolver {
    suspend fun resolve(sourceUrl: String): List<StreamStation>
}