package com.localmusic.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006D77),
    secondary = Color(0xFF2F5D62),
    tertiary = Color(0xFF8A5A44),
    background = Color(0xFFFCFCF8),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF1B1B18),
    onSurface = Color(0xFF1B1B18)
)

/** Metro-influenced Material 3 theme foundation. */
@Composable
fun LocalMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
