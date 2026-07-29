package com.localmusic.player.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localmusic.player.ui.theme.AppThemeMode

@Composable
internal fun SettingsContent(
        uiState: HomeUiState,
        onThemeSelected: (AppThemeMode) -> Unit,
        onRemoveFolderSource: (String) -> Unit,
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
