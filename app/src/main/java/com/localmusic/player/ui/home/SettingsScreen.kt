package com.localmusic.player.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
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
    }
}
