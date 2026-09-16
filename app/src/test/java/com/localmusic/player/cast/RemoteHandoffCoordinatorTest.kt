package com.localmusic.player.cast

import com.localmusic.player.domain.model.RemoteStreamState
import com.localmusic.player.domain.model.RemoteStreamStatus
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SongSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteHandoffCoordinatorTest {
    private val song =
        Song(
            id = "station",
            uri = "https://example.com/live.mp3",
            title = "Station",
            artist = "Example",
            album = "",
            durationMillis = 0,
            dateAddedEpochSeconds = 0,
            folderName = "Online streams",
            source = SongSource.STREAM
        )

    @Test
    fun pausesOnlyOnceForMatchingPlayingRequest() {
        val coordinator = RemoteHandoffCoordinator()
        val state = matchingState(RemoteStreamStatus.Playing)

        assertTrue(coordinator.shouldPauseLocal(state, song))
        coordinator.markLocalPaused(state)
        assertFalse(coordinator.shouldPauseLocal(state, song))
    }

    @Test
    fun doesNotPauseWhileConnectingOrForStaleRequest() {
        val coordinator = RemoteHandoffCoordinator()

        assertFalse(coordinator.shouldPauseLocal(matchingState(RemoteStreamStatus.Connecting), song))
        assertFalse(
            coordinator.shouldPauseLocal(
                matchingState(RemoteStreamStatus.Playing).copy(requestedUri = "https://example.com/old.mp3"),
                song
            )
        )
    }

    @Test
    fun permitsNewHandoffAfterDisconnect() {
        val coordinator = RemoteHandoffCoordinator()
        val playing = matchingState(RemoteStreamStatus.Playing)
        coordinator.markLocalPaused(playing)

        assertFalse(coordinator.shouldPauseLocal(playing, song))
        assertFalse(coordinator.shouldPauseLocal(RemoteStreamState(), song))
        assertTrue(coordinator.shouldPauseLocal(playing, song))
    }

    private fun matchingState(status: RemoteStreamStatus) =
        RemoteStreamState(
            status = status,
            requestedSongId = song.id,
            requestedUri = song.uri
        )
}