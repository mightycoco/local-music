package com.localmusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.localmusic.player.data.database.LocalMusicDatabase
import com.localmusic.player.data.mediastore.MediaStoreMusicScanner
import com.localmusic.player.data.repository.RoomMusicRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localmusic.player.domain.usecase.ObserveSongsUseCase
import com.localmusic.player.domain.usecase.RefreshMusicLibraryUseCase
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
        val repository = RoomMusicRepository(
            songDao = database.songDao(),
            musicScanner = MediaStoreMusicScanner(contentResolver)
        )
        val factory = HomeViewModelFactory(
            observeSongs = ObserveSongsUseCase(repository),
            refreshMusicLibrary = RefreshMusicLibraryUseCase(repository)
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
