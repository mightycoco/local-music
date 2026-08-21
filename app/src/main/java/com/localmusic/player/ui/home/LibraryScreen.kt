package com.localmusic.player.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.graphics.Color
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
    libraryActions: LibraryActions,
    songActions: SongActions,
    createPlaylist: (String) -> Unit
) {
    OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = libraryActions.updateSearch,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Search") }
    )
    Spacer(modifier = Modifier.height(12.dp))
    if (uiState.songs.isEmpty()) {
        Button(onClick = libraryActions.addFolderSource) { Text("Add Folder") }
        Spacer(modifier = Modifier.height(12.dp))
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterRow(
                uiState = uiState,
                onFilterSelected = libraryActions.selectFilter,
                modifier = Modifier.weight(1f)
            )
            SortOrderDropdown(
                selectedSortOrder = uiState.sortOrder,
                onSortSelected = libraryActions.selectSortOrder
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    val songListActions =
        songActions.forList(createPlaylist = createPlaylist)
    AdaptiveLibraryContent(
        uiState = uiState,
        listState = listState,
        onBrowseValueSelected = libraryActions.selectBrowseValue,
        onLoadNextPage = libraryActions.loadNextPage,
        songListActions = songListActions
    )
}

@Composable
private fun FilterRow(
    uiState: HomeUiState,
    onFilterSelected: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LibraryFilter.entries.forEach { filter ->
            AssistChip(
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                enabled = filter != uiState.selectedFilter,
                border =
                    AssistChipDefaults.assistChipBorder(
                        enabled = filter != uiState.selectedFilter,
                        borderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent
                    )
            )
        }
    }
}

@Composable
private fun SortOrderDropdown(selectedSortOrder: SortOrder, onSortSelected: (SortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = "${selectedSortOrder.label} ▼", style = MaterialTheme.typography.bodySmall)
        }
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
    onLoadNextPage: () -> Unit,
    songListActions: SongListActions
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
                    isPlaying = uiState.isPlaying,
                    listState = listState,
                    modifier = Modifier.weight(1f),
                    playlists = uiState.importedPlaylists,
                    onLoadNextPage = onLoadNextPage,
                    actions = songListActions
                )
                SongList(
                    songs = uiState.favouriteSongs,
                    artworkBySongId = uiState.artworkBySongId,
                    nowPlayingSongId = uiState.nowPlayingSong?.id,
                    isPlaying = uiState.isPlaying,
                    emptyTitle = "No favourites yet",
                    emptyMessage = "Mark songs or online streams as favourites to pin them here.",
                    modifier = Modifier.width(320.dp),
                    playlists = uiState.importedPlaylists,
                    actions = songListActions
                )
            }
        } else {
            SongList(
                songs = uiState.songs,
                artworkBySongId = uiState.artworkBySongId,
                nowPlayingSongId = uiState.nowPlayingSong?.id,
                isPlaying = uiState.isPlaying,
                listState = listState,
                playlists = uiState.importedPlaylists,
                onLoadNextPage = onLoadNextPage,
                actions = songListActions
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
    val valueCounts = LibraryBrowser.valueCounts(songs, filter)
    if (valueCounts.isEmpty()) {
        EmptyState(
            title = "No ${filter.label.lowercase()} found",
            message = "Refresh your local library or choose another filter."
        )
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(valueCounts, key = { it.first.lowercase() }) { (value, songCount) ->
            ListItem(
                modifier = Modifier.clickable { onBrowseValueSelected(value) },
                headlineContent = { Text(value) },
                supportingContent = { Text("$songCount songs") }
            )
        }
    }
}
