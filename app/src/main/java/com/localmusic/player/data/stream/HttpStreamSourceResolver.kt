package com.localmusic.player.data.stream

import com.localmusic.player.domain.model.StreamStation
import com.localmusic.player.domain.repository.StreamSourceResolver
import com.localmusic.player.playlist.M3uPlaylistCodec
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bounded HTTP(S) resolver for direct stations and extended M3U resources. */
class HttpStreamSourceResolver(
    private val playlistCodec: M3uPlaylistCodec = M3uPlaylistCodec(),
    private val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StreamSourceResolver {
    override suspend fun resolve(sourceUrl: String): List<StreamStation> =
        withContext(ioDispatcher) {
            val normalizedUrl = validateHttpUrl(sourceUrl)
            if (!isM3uUrl(normalizedUrl)) return@withContext listOf(directStation(normalizedUrl))

            val content = downloadPlaylist(normalizedUrl)
            playlistCodec.parse("Remote stream source", content).entries
                .asSequence()
                .filter { entry -> isHttpUrl(entry.uri) }
                .map { entry ->
                    StreamStation(
                        title = entry.title.ifBlank { hostName(entry.uri) },
                        artist = entry.artist.ifBlank { "Online radio" },
                        durationSeconds = entry.durationSeconds,
                        uri = normalizeHttpUrl(entry.uri)
                    )
                }
                .distinctBy(StreamStation::uri)
                .toList()
                .ifEmpty { throw IllegalArgumentException("The M3U source contains no HTTP or HTTPS streams") }
        }

    private fun downloadPlaylist(url: String): String {
        val connection = connectionFactory(URL(url))
        return try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.requestMethod = "GET"
            connection.setRequestProperty(
                "Accept",
                "audio/x-mpegurl, audio/mpegurl, application/vnd.apple.mpegurl, text/plain"
            )
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Unable to download M3U source (HTTP ${connection.responseCode})")
            }
            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                val content = StringBuilder()
                var readCharacters = 0
                val buffer = CharArray(4096)
                while (readCharacters < MAX_PLAYLIST_CHARACTERS) {
                    val count = reader.read(buffer, 0, minOf(buffer.size, MAX_PLAYLIST_CHARACTERS - readCharacters))
                    if (count < 0) break
                    content.append(buffer, 0, count)
                    readCharacters += count
                }
                content.toString()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun directStation(url: String) =
        StreamStation(
            title = hostName(url),
            artist = "Online radio",
            durationSeconds = -1L,
            uri = url
        )

    private fun validateHttpUrl(value: String): String {
        val normalized = normalizeHttpUrl(value)
        require(isHttpUrl(normalized)) { "Enter an HTTP or HTTPS stream URL" }
        return normalized
    }

    private fun normalizeHttpUrl(value: String): String = URI(value.trim()).normalize().toString()

    private fun isHttpUrl(value: String): Boolean =
        runCatching { URI(value).scheme?.lowercase() in HTTP_SCHEMES && !URI(value).host.isNullOrBlank() }
            .getOrDefault(false)

    private fun isM3uUrl(url: String): Boolean =
        URI(url).path?.substringAfterLast('.', "")?.lowercase() in M3U_EXTENSIONS

    private fun hostName(url: String): String = URI(url).host ?: "Online stream"

    private companion object {
        val HTTP_SCHEMES = setOf("http", "https")
        val M3U_EXTENSIONS = setOf("m3u", "m3u8")
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
        const val MAX_PLAYLIST_CHARACTERS = 1_000_000
    }
}