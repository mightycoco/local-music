package com.localmusic.player.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.ui.theme.AppThemeMode

@Composable
internal fun SettingsContent(
        uiState: HomeUiState,
        onThemeSelected: (AppThemeMode) -> Unit,
        onRemoveFolderSource: (String) -> Unit,
        onClearArtworkCache: () -> Unit,
        onDefaultFilterSelected: (LibraryFilter) -> Unit,
        onDefaultSortOrderSelected: (SortOrder) -> Unit,
        onExternalArtworkDownloadEnabledChange: (Boolean) -> Unit
) {
    Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Theme", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { mode ->
                AssistChip(
                        onClick = { onThemeSelected(mode) },
                        label = { Text(mode.label) },
                        enabled = mode != uiState.themeMode
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Download missing artwork", style = MaterialTheme.typography.bodyLarge)
                Text(
                        text =
                                "After embedded, folder, cover, and cached art fail, use MusicBrainz and Cover Art Archive. Artist and album metadata is sent to these services; artwork is cached locally.",
                        style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                    checked = uiState.isExternalArtworkDownloadEnabled,
                    onCheckedChange = onExternalArtworkDownloadEnabledChange
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Library defaults", style = MaterialTheme.typography.bodyLarge)
        LibraryDefaultMenu(
                label = "Filter",
                selectedLabel = uiState.selectedFilter.label,
                options = LibraryFilter.entries,
                optionLabel = LibraryFilter::label,
                onSelected = onDefaultFilterSelected
        )
        LibraryDefaultMenu(
                label = "Sort order",
                selectedLabel = uiState.sortOrder.label,
                options = SortOrder.entries,
                optionLabel = SortOrder::label,
                onSelected = onDefaultSortOrderSelected
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Artwork cache", style = MaterialTheme.typography.bodyLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                    text = formatCacheSize(uiState.artworkCacheSizeBytes),
                    style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                    onClick = onClearArtworkCache,
                    enabled = uiState.artworkCacheSizeBytes > 0L
            ) { Text("Clear") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Local folders", style = MaterialTheme.typography.bodyLarge)
        if (uiState.folderSourceUris.isEmpty()) {
            Text(
                    text = "No additional folders selected.",
                    style = MaterialTheme.typography.bodySmall
            )
        } else {
            uiState.folderSourceUris.forEach { folderUri ->
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                            text = folderUri,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { onRemoveFolderSource(folderUri) }) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
private fun <T> LibraryDefaultMenu(
        label: String,
        selectedLabel: String,
        options: List<T>,
        optionLabel: (T) -> String,
        onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("$label: $selectedLabel") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        }
                )
            }
        }
    }
}

private fun formatCacheSize(sizeBytes: Long): String =
        when {
            sizeBytes < 1024L -> "$sizeBytes B"
            sizeBytes < 1024L * 1024L -> "${sizeBytes / 1024L} KB"
            else -> "${sizeBytes / (1024L * 1024L)} MB"
        }
