package com.localmusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import com.localmusic.player.artwork.ArtworkDiskCache
import com.localmusic.player.artwork.ArtworkPreferences
import com.localmusic.player.artwork.EmbeddedArtworkExtractor
import com.localmusic.player.bluetooth.CarModePreferences
import com.localmusic.player.data.database.LocalMusicDatabase
import com.localmusic.player.data.mediastore.CompositeMusicScanner
import com.localmusic.player.data.mediastore.MediaStoreMusicScanner
import com.localmusic.player.data.repository.RoomMusicRepository
import com.localmusic.player.data.saf.SafFolderMusicScanner
import com.localmusic.player.data.saf.SharedPreferencesSafFolderSourceStore
import com.localmusic.player.domain.usecase.AddFolderSourceUseCase
import com.localmusic.player.domain.usecase.ObserveSongsUseCase
import com.localmusic.player.domain.usecase.RefreshMusicLibraryUseCase
import com.localmusic.player.domain.usecase.RemoveFolderSourceUseCase
import com.localmusic.player.domain.usecase.SetFavouriteUseCase
import com.localmusic.player.domain.usecase.StartPlaybackUseCase
import com.localmusic.player.playback.Media3PlaybackController
import com.localmusic.player.playlist.RoomPlaylistStore
import com.localmusic.player.playlist.SharedPreferencesPlaylistStore
import com.localmusic.player.settings.LibraryPreferences
import com.localmusic.player.ui.home.HomeRoute
import com.localmusic.player.ui.home.HomeViewModel
import com.localmusic.player.ui.home.HomeViewModelFactory
import com.localmusic.player.ui.theme.LocalMusicTheme

/** Main Android entry point for the local music player. */
@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database =
                Room.databaseBuilder(
                                applicationContext,
                                LocalMusicDatabase::class.java,
                                "local-music.db"
                        )
                        .addMigrations(
                                LocalMusicDatabase.MIGRATION_1_2,
                                LocalMusicDatabase.MIGRATION_2_3,
                                LocalMusicDatabase.MIGRATION_3_4
                        )
                        .build()
        val safFolderSourceStore =
                SharedPreferencesSafFolderSourceStore(
                        getSharedPreferences("local-music-sources", MODE_PRIVATE)
                )
        val repository =
                RoomMusicRepository(
                        songDao = database.songDao(),
                        musicScanner =
                                CompositeMusicScanner(
                                        listOf(
                                                MediaStoreMusicScanner(contentResolver),
                                                SafFolderMusicScanner(
                                                        applicationContext,
                                                        safFolderSourceStore
                                                )
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
        val artworkCache = ArtworkDiskCache(filesDir)
        val factory =
                HomeViewModelFactory(
                        observeSongs = ObserveSongsUseCase(repository),
                        refreshMusicLibrary = RefreshMusicLibraryUseCase(repository),
                        addFolderSource = AddFolderSourceUseCase(repository),
                        removeFolderSource = RemoveFolderSourceUseCase(repository),
                        folderSourceUris = safFolderSourceStore::folders,
                        setFavourite = SetFavouriteUseCase(repository),
                        startPlayback =
                                StartPlaybackUseCase(Media3PlaybackController(applicationContext)),
                        playlistStore =
                                RoomPlaylistStore(
                                        database.playlistDao(),
                                        SharedPreferencesPlaylistStore(
                                                getSharedPreferences(
                                                        "local-music-playlists",
                                                        MODE_PRIVATE
                                                )
                                        )
                                ),
                        artworkExtractor =
                                EmbeddedArtworkExtractor(
                                        context = applicationContext,
                                        cache = artworkCache
                                ),
                        artworkCache = artworkCache,
                        artworkPreferences =
                                ArtworkPreferences(
                                        getSharedPreferences("local-music-artwork", MODE_PRIVATE)
                                ),
                        libraryPreferences =
                                LibraryPreferences(
                                        getSharedPreferences("local-music-library", MODE_PRIVATE)
                                ),
                        carModePreferences =
                                CarModePreferences(
                                        getSharedPreferences("local-music-car-mode", MODE_PRIVATE)
                                )
                )

        setContent {
            val viewModel: HomeViewModel = viewModel(factory = factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isMiniPlayerVisible =
                    uiState.nowPlayingSong != null &&
                            uiState.selectedScreen !=
                                    com.localmusic.player.ui.home.HomeScreenDestination.NowPlaying
            DisposableEffect(isMiniPlayerVisible) {
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                if (isMiniPlayerVisible) {
                    insetsController.hide(WindowInsetsCompat.Type.statusBars())
                } else {
                    insetsController.show(WindowInsetsCompat.Type.statusBars())
                }
                onDispose {
                    if (isMiniPlayerVisible) {
                        insetsController.show(WindowInsetsCompat.Type.statusBars())
                    }
                }
            }
            LocalMusicTheme(themeMode = uiState.themeMode) {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) { HomeRoute(viewModel = viewModel, uiState = uiState) }
            }
        }
    }
}
