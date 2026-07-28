package com.localmusic.player.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localmusic.player.domain.model.LibraryBrowser
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder

private val browsableFilters =
        setOf(
                LibraryFilter.Artists,
                LibraryFilter.Albums,
                LibraryFilter.Genres,
                LibraryFilter.Folders
        )

@Composable
internal fun LibraryContent(
        uiState: HomeUiState,
        listState: LazyListState,
        onSearchChange: (String) -> Unit,
        onFilterSelected: (LibraryFilter) -> Unit,
        onBrowseValueSelected: (String?) -> Unit,
        onSortSelected: (SortOrder) -> Unit,
        onAddFolderSource: () -> Unit,
        onImportPlaylist: () -> Unit,
        onExportPlaylist: () -> Unit,
        onSongSelected: (Song) -> Unit,
        onFavouriteToggle: (Song) -> Unit,
        onCreatePlaylist: (String) -> Unit,
        onAddSongToPlaylist: (Song, String) -> Unit,
        onAddSongToQueue: (Song) -> Unit
) {
    OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search") }
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onAddFolderSource) { Text("Add Folder") }
        Button(onClick = onImportPlaylist) { Text("Import M3U") }
        Button(onClick = onExportPlaylist) { Text("Export M3U") }
    }
    Spacer(modifier = Modifier.height(12.dp))
    FilterRow(uiState, onFilterSelected)
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        if (uiState.importedPlaylists.isNotEmpty()) {
            Text(
                    text =
                            "Imported playlists: ${uiState.importedPlaylists.joinToString { it.name }}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        SortOrderDropdown(selectedSortOrder = uiState.sortOrder, onSortSelected = onSortSelected)
    }
    Spacer(modifier = Modifier.height(12.dp))
    AdaptiveLibraryContent(
            uiState = uiState,
            listState = listState,
            onBrowseValueSelected = onBrowseValueSelected,
            onSongSelected = onSongSelected,
            onFavouriteToggle = onFavouriteToggle,
            onCreatePlaylist = onCreatePlaylist,
            onAddSongToPlaylist = onAddSongToPlaylist,
            onAddSongToQueue = onAddSongToQueue
    )
}

@Composable
private fun FilterRow(uiState: HomeUiState, onFilterSelected: (LibraryFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryFilter.entries.forEach { filter ->
                AssistChip(
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter.label) },
                        enabled = filter != uiState.selectedFilter
                )
            }
        }
    }
}

@Composable
private fun SortOrderDropdown(selectedSortOrder: SortOrder, onSortSelected: (SortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) { Text("${selectedSortOrder.label} ▼") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOrder.entries.forEach { sortOrder ->
                DropdownMenuItem(
                        text = { Text(sortOrder.label) },
                        onClick = {
                            onSortSelected(sortOrder)
                            expanded = false
                        }
                )
            }
        }
    }
}

@Composable
private fun AdaptiveLibraryContent(
        uiState: HomeUiState,
        listState: LazyListState,
        onBrowseValueSelected: (String?) -> Unit,
        onSongSelected: (Song) -> Unit,
        onFavouriteToggle: (Song) -> Unit,
        onCreatePlaylist: (String) -> Unit,
        onAddSongToPlaylist: (Song, String) -> Unit,
        onAddSongToQueue: (Song) -> Unit
) {
    if (uiState.selectedFilter in browsableFilters && uiState.selectedBrowseValue == null) {
        BrowseFacetList(
                filter = uiState.selectedFilter,
                songs = uiState.songs,
                onBrowseValueSelected = onBrowseValueSelected
        )
        return
    }

    uiState.selectedBrowseValue?.let {
        TextButton(onClick = { onBrowseValueSelected(null) }) {
            Text("Back to ${uiState.selectedFilter.label}")
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SongList(
                        songs = uiState.songs,
                        artworkBySongId = uiState.artworkBySongId,
                        nowPlayingSongId = uiState.nowPlayingSong?.id,
                        listState = listState,
                        modifier = Modifier.weight(1f),
                        onSongSelected = onSongSelected,
                        onFavouriteToggle = onFavouriteToggle,
                        playlists = uiState.importedPlaylists,
                        onCreatePlaylist = onCreatePlaylist,
                        onAddSongToPlaylist = onAddSongToPlaylist,
                        onAddSongToQueue = onAddSongToQueue
                )
                SongList(
                        songs = uiState.songs.filter { it.isFavourite },
                        artworkBySongId = uiState.artworkBySongId,
                        nowPlayingSongId = uiState.nowPlayingSong?.id,
                        emptyTitle = "No favourites yet",
                        emptyMessage = "Mark local songs as favourites to pin them here.",
                        modifier = Modifier.width(320.dp),
                        onSongSelected = onSongSelected,
                        onFavouriteToggle = onFavouriteToggle,
                        playlists = uiState.importedPlaylists,
                        onCreatePlaylist = onCreatePlaylist,
                        onAddSongToPlaylist = onAddSongToPlaylist,
                        onAddSongToQueue = onAddSongToQueue
                )
            }
        } else {
            SongList(
                    songs = uiState.songs,
                    artworkBySongId = uiState.artworkBySongId,
                    nowPlayingSongId = uiState.nowPlayingSong?.id,
                    listState = listState,
                    onSongSelected = onSongSelected,
                    onFavouriteToggle = onFavouriteToggle,
                    playlists = uiState.importedPlaylists,
                    onCreatePlaylist = onCreatePlaylist,
                    onAddSongToPlaylist = onAddSongToPlaylist,
                    onAddSongToQueue = onAddSongToQueue
            )
        }
    }
}

@Composable
private fun BrowseFacetList(
        filter: LibraryFilter,
        songs: List<Song>,
        onBrowseValueSelected: (String) -> Unit
) {
    val values = LibraryBrowser.values(songs, filter)
    if (values.isEmpty()) {
        EmptyState(
                title = "No ${filter.label.lowercase()} found",
                message = "Refresh your local library or choose another filter."
        )
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(values) { value ->
            val songCount = songs.count { LibraryBrowser.matches(it, filter, value) }
            ListItem(
                    modifier = Modifier.clickable { onBrowseValueSelected(value) },
                    headlineContent = { Text(value) },
                    supportingContent = { Text("$songCount songs") }
            )
        }
    }
}
