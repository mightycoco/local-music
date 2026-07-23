package com.localmusic.player.artwork

import com.localmusic.player.domain.model.Song
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/** Downloads artwork metadata only after local artwork sources have been exhausted. */
interface ExternalArtworkProvider {
    fun artworkFor(song: Song): ByteArray?
}

/** MusicBrainz release search with Cover Art Archive downloads and an artist-release fallback. */
class MusicBrainzCoverArtProvider(
    private val transport: ArtworkHttpTransport = UrlConnectionArtworkHttpTransport()
) : ExternalArtworkProvider {
    override fun artworkFor(song: Song): ByteArray? {
        if (song.artist.isUnknown() || song.album.isUnknown()) return null

        releaseId("release:\"${song.album}\" AND artist:\"${song.artist}\"")
            ?.let(::coverArtForRelease)
            ?.let { return it }

        return releaseId("artist:\"${song.artist}\" AND primarytype:album")
            ?.let(::coverArtForRelease)
    }

    private fun releaseId(query: String): String? {
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        val response = transport.get("https://musicbrainz.org/ws/2/release?fmt=json&limit=1&query=$encodedQuery")
            ?: return null
        return RELEASE_ID_PATTERN.find(response.toString(Charsets.UTF_8))
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
    }

    private fun coverArtForRelease(releaseId: String): ByteArray? =
        transport.get("https://coverartarchive.org/release/$releaseId/front-250")

    private fun String.isUnknown(): Boolean = isBlank() || startsWith("Unknown", ignoreCase = true)

    private companion object {
        val RELEASE_ID_PATTERN = Regex("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
    }
}

interface ArtworkHttpTransport {
    fun get(url: String): ByteArray?
}

class UrlConnectionArtworkHttpTransport : ArtworkHttpTransport {
    override fun get(url: String): ByteArray? {
        rateLimiter.awaitTurn()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "LocalMusic/0.1.0 (local artwork lookup)")
            setRequestProperty("Accept", "application/json, image/*")
        }
        return try {
            if (connection.responseCode !in 200..299) null else connection.inputStream.use { it.readBytes() }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        val rateLimiter = ArtworkRequestRateLimiter()
    }
}

class ArtworkRequestRateLimiter(
    private val minimumIntervalMillis: Long = 1_100L,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sleep: (Long) -> Unit = Thread::sleep
) {
    private var lastRequestMillis = Long.MIN_VALUE

    @Synchronized
    fun awaitTurn() {
        val elapsed = clock() - lastRequestMillis
        if (lastRequestMillis != Long.MIN_VALUE && elapsed < minimumIntervalMillis) {
            sleep(minimumIntervalMillis - elapsed)
        }
        lastRequestMillis = clock()
    }
}