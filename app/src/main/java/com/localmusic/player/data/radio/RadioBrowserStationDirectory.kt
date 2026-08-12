package com.localmusic.player.data.radio

import com.localmusic.player.domain.model.RadioStation
import com.localmusic.player.domain.repository.RadioStationDirectory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Bounded Radio Browser client that searches active stations by name. */
class RadioBrowserStationDirectory(
    private val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val serverHosts: List<String> = DEFAULT_SERVER_HOSTS
) : RadioStationDirectory {
    override suspend fun search(query: String): List<RadioStation> =
        withContext(ioDispatcher) {
            val normalizedQuery = query.trim()
            require(normalizedQuery.isNotEmpty()) { "Enter a station name to search" }

            val path =
                "/json/stations/search?name=${URLEncoder.encode(normalizedQuery, Charsets.UTF_8.name())}" +
                        "&hidebroken=true&order=clickcount&reverse=true&limit=$RESULT_LIMIT"
            val response = serverHosts.asSequence().mapNotNull { host ->
                runCatching { download("https://$host$path") }.getOrNull()
            }.firstOrNull() ?: throw IllegalStateException("Radio directory is unavailable")

            Json.parseToJsonElement(response).jsonArray.mapNotNull { element ->
                val station = element.jsonObject
                val id = station["stationuuid"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val name = station["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val streamUrl = station["url_resolved"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                if (id.isBlank() || name.isBlank() || !isHttpUrl(streamUrl)) return@mapNotNull null

                RadioStation(
                    id = id,
                    name = name,
                    streamUrl = URI(streamUrl).normalize().toString(),
                    faviconUrl =
                        station["favicon"]?.jsonPrimitive?.contentOrNull
                            ?.trim()
                            ?.takeIf(::isHttpUrl)
                )
            }.distinctBy(RadioStation::streamUrl)
        }

    private fun download(url: String): String {
        val connection = connectionFactory(URL(url))
        return try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Radio directory request failed (HTTP ${connection.responseCode})")
            }
            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                val content = StringBuilder()
                val buffer = CharArray(4096)
                while (content.length < MAX_RESPONSE_CHARACTERS) {
                    val read = reader.read(buffer, 0, minOf(buffer.size, MAX_RESPONSE_CHARACTERS - content.length))
                    if (read < 0) break
                    content.append(buffer, 0, read)
                }
                content.toString()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun isHttpUrl(value: String): Boolean =
        runCatching { URI(value).scheme?.lowercase() in HTTP_SCHEMES && !URI(value).host.isNullOrBlank() }
            .getOrDefault(false)

    private companion object {
        val DEFAULT_SERVER_HOSTS = listOf("de1.api.radio-browser.info", "nl1.api.radio-browser.info")
        val HTTP_SCHEMES = setOf("http", "https")
        const val USER_AGENT = "LocalMusic/0.1.0"
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
        const val MAX_RESPONSE_CHARACTERS = 1_000_000
        const val RESULT_LIMIT = 50
    }
}