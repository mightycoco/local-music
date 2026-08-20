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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.localmusic.player.BuildConfig
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.settings.OpenSourceLicenseNotice
import com.localmusic.player.settings.OpenSourceLicenses
import com.localmusic.player.ui.theme.AppThemeMode

@Composable
internal fun SettingsContent(
    uiState: HomeUiState,
    actions: SettingsFeatureActions
) {
    var selectedLicenseNotice by remember { mutableStateOf<OpenSourceLicenseNotice?>(null) }
    Column(
        modifier = Modifier.padding(top = 24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Theme", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { mode ->
                AssistChip(
                    onClick = { actions.appearance.selectTheme(mode) },
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
                onCheckedChange = actions.appearance.setExternalArtworkDownloadEnabled
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Prefer visualizer", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text =
                        "Show the visualizer on Now Playing even when cover artwork is available.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = uiState.isVisualizerPreferred,
                onCheckedChange = actions.appearance.setVisualizerPreferred
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Library defaults", style = MaterialTheme.typography.bodyLarge)
        LibraryDefaultMenu(
            label = "Filter",
            selectedLabel = uiState.selectedFilter.label,
            options = LibraryFilter.entries,
            optionLabel = LibraryFilter::label,
            onSelected = actions.preferences.setDefaultFilter
        )
        LibraryDefaultMenu(
            label = "Sort order",
            selectedLabel = uiState.sortOrder.label,
            options = SortOrder.entries,
            optionLabel = SortOrder::label,
            onSelected = actions.preferences.setDefaultSortOrder
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Car Mode", style = MaterialTheme.typography.bodyLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Enable Car Mode", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text =
                        "Use simplified playback controls when driving. Bluetooth car detection can also enable this mode.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = uiState.isCarModeManuallyEnabled,
                onCheckedChange = actions.preferences.setCarModeManuallyEnabled
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Keep display on", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text =
                        "Prevent sleep while charging or connected to detected car audio.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = uiState.isKeepDisplayOnEnabled,
                onCheckedChange = actions.preferences.setKeepDisplayOnEnabled
            )
        }
        if (uiState.connectedCarAudioDevices.isEmpty()) {
            Text(
                text = "Connect an A2DP device to mark it as car audio.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            uiState.connectedCarAudioDevices.forEach { device ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = device.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text =
                                if (device.isLikelyCarDevice) "Detected as car audio"
                                else "Mark this device to enable Car Mode when connected",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = device.id in uiState.markedCarDeviceIds,
                        onCheckedChange = { marked ->
                            actions.preferences.setCarDeviceMarked(device.id, marked)
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Artwork cache", style = MaterialTheme.typography.bodyLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatCacheSize(uiState.artworkCacheSizeBytes),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = actions.appearance.clearArtworkCache,
                enabled = uiState.artworkCacheSizeBytes > 0L
            ) { Text("Clear") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Local folders", style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = actions.folders.add) { Text("Add Folder") }
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
                    OutlinedButton(onClick = { actions.folders.remove(folderUri) }) {
                        Text("Remove")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "About", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Local Music ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = { selectedLicenseNotice = OpenSourceLicenses.notices.first() }) {
            Text("Open source licenses")
        }
    }

    selectedLicenseNotice?.let { notice ->
        LicenseNoticeDialog(notice = notice, onDismiss = { selectedLicenseNotice = null })
    }
}

@Composable
private fun LicenseNoticeDialog(notice: OpenSourceLicenseNotice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(notice.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(notice.copyright, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Text(notice.licenseName, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(notice.licenseText, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } }
    )
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
