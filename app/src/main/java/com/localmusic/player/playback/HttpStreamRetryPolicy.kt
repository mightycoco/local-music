package com.localmusic.player.playback

import androidx.media3.common.C
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import java.net.URI

internal class HttpStreamRetryPolicy : DefaultLoadErrorHandlingPolicy() {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        if (!isHttpStream(loadErrorInfo.loadEventInfo.dataSpec.uri.toString())) {
            return super.getRetryDelayMsFor(loadErrorInfo)
        }

        return retryDelayMillis(loadErrorInfo.errorCount)
    }

    internal companion object {
        private val HTTP_SCHEMES = setOf("http", "https")
        private val RETRY_DELAYS_MILLIS = longArrayOf(1_000L, 2_000L, 4_000L)

        fun retryDelayMillis(errorCount: Int): Long =
            RETRY_DELAYS_MILLIS.getOrNull(errorCount - 1) ?: C.TIME_UNSET

        fun isHttpStream(uri: String): Boolean =
            runCatching { URI(uri).scheme?.lowercase() in HTTP_SCHEMES }.getOrDefault(false)
    }
}