package com.localmusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.localmusic.player.artwork.ArtworkDiskCache
import com.localmusic.player.artwork.EmbeddedArtworkExtractor
import com.localmusic.player.data.database.LocalMusicDatabase
import com.localmusic.player.data.mediastore.CompositeMusicScanner
import com.localmusic.player.data.mediastore.MediaStoreMusicScanner
import com.localmusic.player.data.saf.SafFolderMusicScanner
import com.localmusic.player.data.saf.SharedPreferencesSafFolderSourceStore
import com.localmusic.player.data.repository.RoomMusicRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localmusic.player.domain.usecase.AddFolderSourceUseCase
import com.localmusic.player.domain.usecase.ObserveSongsUseCase
import com.localmusic.player.domain.usecase.RefreshMusicLibraryUseCase
import com.localmusic.player.domain.usecase.SetFavouriteUseCase
import com.localmusic.player.domain.usecase.StartPlaybackUseCase
import com.localmusic.player.playback.Media3PlaybackController
import com.localmusic.player.ui.home.HomeRoute
import com.localmusic.player.ui.home.HomeViewModel
import com.localmusic.player.ui.home.HomeViewModelFactory
import com.localmusic.player.ui.theme.LocalMusicTheme

/** Main Android entry point for the local music player. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = Room.databaseBuilder(
            applicationContext,
            LocalMusicDatabase::class.java,
            "local-music.db"
        ).build()
        val safFolderSourceStore = SharedPreferencesSafFolderSourceStore(
            getSharedPreferences("local-music-sources", MODE_PRIVATE)
        )
        val repository = RoomMusicRepository(
            songDao = database.songDao(),
            musicScanner = CompositeMusicScanner(
                listOf(
                    MediaStoreMusicScanner(contentResolver),
                    SafFolderMusicScanner(applicationContext, safFolderSourceStore)
                )
            ),
            safFolderSourceStore = safFolderSourceStore,
            persistSafFolderPermission = { folderUri ->
                contentResolver.takePersistableUriPermission(
                    android.net.Uri.parse(folderUri),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        )
        val factory = HomeViewModelFactory(
            observeSongs = ObserveSongsUseCase(repository),
            refreshMusicLibrary = RefreshMusicLibraryUseCase(repository),
            addFolderSource = AddFolderSourceUseCase(repository),
            setFavourite = SetFavouriteUseCase(repository),
            startPlayback = StartPlaybackUseCase(Media3PlaybackController(applicationContext)),
            artworkExtractor = EmbeddedArtworkExtractor(
                context = applicationContext,
                cache = ArtworkDiskCache(cacheDir)
            )
        )

        setContent {
            LocalMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: HomeViewModel = viewModel(factory = factory)
                    HomeRoute(viewModel = viewModel)
                }
            }
        }
    }
}
