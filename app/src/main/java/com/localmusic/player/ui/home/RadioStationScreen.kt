package com.localmusic.player.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.localmusic.player.domain.model.RadioStation

@Composable
internal fun RadioStationScreen(
    uiState: HomeUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPlay: (RadioStation) -> Unit,
    onAddToPlaylist: (RadioStation, String) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var stationToAdd by remember { mutableStateOf<RadioStation?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back" }) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
        }
        Text(
            text = "Radio stations",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        OutlinedTextField(
            value = uiState.radioSearchQuery,
            onValueChange = onQueryChange,
            label = { Text("Search Radio Browser") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onSearch, enabled = !uiState.isRadioSearchLoading) {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search")
                }
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        when {
            uiState.isRadioSearchLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Searching stations...")
            }

            uiState.radioSearchError != null -> EmptyState(
                title = "Search unavailable",
                message = uiState.radioSearchError,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            uiState.hasSearchedRadioStations && uiState.radioStations.isEmpty() -> EmptyState(
                title = "No stations found",
                message = "Try another station name or genre.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            uiState.radioStations.isEmpty() -> EmptyState(
                title = "Find a radio station",
                message = "Search the Radio Browser directory.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.radioStations, key = RadioStation::id) { station ->
                    RadioStationRow(
                        station = station,
                        onPlay = onPlay,
                        onAddToPlaylist = { stationToAdd = station }
                    )
                }
            }
        }
    }
    stationToAdd?.let { station ->
        PlaylistChooserDialog(
            playlists = uiState.importedPlaylists,
            onDismiss = { stationToAdd = null },
            onPlaylistSelected = { playlistName ->
                onAddToPlaylist(station, playlistName)
                stationToAdd = null
            },
            onCreatePlaylist = { playlistName ->
                onCreatePlaylist(playlistName)
                onAddToPlaylist(station, playlistName)
                stationToAdd = null
            }
        )
    }
}

@Composable
private fun RadioStationRow(
    station: RadioStation,
    onPlay: (RadioStation) -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        leadingContent = {
            AsyncImage(
                model = station.faviconUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        headlineContent = { Text(station.name, maxLines = 1) },
        trailingContent = {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.semantics {
                        contentDescription = "Station actions"
                    }
                ) {
                    Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Play") },
                        onClick = {
                            showMenu = false
                            onPlay(station)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        onClick = {
                            showMenu = false
                            onAddToPlaylist()
                        }
                    )
                }
            }
        }
    )
}