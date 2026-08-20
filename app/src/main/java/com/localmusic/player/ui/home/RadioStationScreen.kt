package com.localmusic.player.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.localmusic.player.domain.model.RadioStation

@Composable
internal fun RadioStationScreen(
    uiState: HomeUiState,
    onBack: () -> Unit,
    actions: RadioActions,
    onAddToCurrentPlaylist: (RadioStation) -> Unit,
    createPlaylist: (String) -> Unit
) {
    var stationToAddToNewPlaylist by remember { mutableStateOf<RadioStation?>(null) }
    var searchFieldValue by remember { mutableStateOf(TextFieldValue(uiState.radioSearchQuery)) }
    val keyboardController = LocalSoftwareKeyboardController.current
    fun submitSearch() {
        keyboardController?.hide()
        actions.search()
    }

    if (searchFieldValue.text != uiState.radioSearchQuery) {
        searchFieldValue = searchFieldValue.copy(text = uiState.radioSearchQuery)
    }

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
            value = searchFieldValue,
            onValueChange = { value ->
                searchFieldValue = value
                actions.updateQuery(value.text)
            },
            label = { Text("Search Radio Browser") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
            trailingIcon = {
                IconButton(onClick = ::submitSearch, enabled = !uiState.isRadioSearchLoading) {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        searchFieldValue = searchFieldValue.copy(
                            selection = TextRange(0, searchFieldValue.text.length)
                        )
                    }
                }
        )
        uiState.previewRadioStation?.let { station ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = actions.stopPreview,
                    modifier = Modifier.semantics { contentDescription = "Stop preview" }
                ) {
                    Icon(imageVector = Icons.Filled.Stop, contentDescription = null)
                }
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
            }
        }
        uiState.radioPreviewError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
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
                        onPreview = actions.preview,
                        onAddToCurrentPlaylist = { onAddToCurrentPlaylist(station) },
                        onAddToNewPlaylist = { stationToAddToNewPlaylist = station }
                    )
                }
            }
        }
    }
    stationToAddToNewPlaylist?.let { station ->
        NewPlaylistNameDialog(
            onDismiss = { stationToAddToNewPlaylist = null },
            onConfirm = { playlistName ->
                createPlaylist(playlistName)
                actions.addToPlaylist(station, playlistName.trim())
                stationToAddToNewPlaylist = null
            }
        )
    }
}

@Composable
private fun NewPlaylistNameDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to new Playlist") },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                singleLine = true,
                label = { Text("Playlist name") }
            )
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Button(onClick = { onConfirm(playlistName) }, enabled = playlistName.isNotBlank()) {
                Text("Add")
            }
        }
    )
}

@Composable
private fun RadioStationRow(
    station: RadioStation,
    onPreview: (RadioStation) -> Unit,
    onAddToCurrentPlaylist: () -> Unit,
    onAddToNewPlaylist: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.clickable { onPreview(station) },
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
                        text = { Text("Add to Playlist") },
                        onClick = {
                            showMenu = false
                            onAddToCurrentPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to new Playlist") },
                        onClick = {
                            showMenu = false
                            onAddToNewPlaylist()
                        }
                    )
                }
            }
        }
    )
}