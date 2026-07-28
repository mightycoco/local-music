package com.localmusic.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
        lightColorScheme(
                primary = Color(0xFF006D77), // #006D77
                secondary = Color(0xFF2F5D62), // #2F5D62
                tertiary = Color(0xFF8A5A44), // #8A5A44
                background = Color(0xFFFCFCF8), // #FCFCF8
                surface = Color(0xFFFFFFFF), // #FFFFFF
                onPrimary = Color.White, // #FFFFFF
                onBackground = Color(0xFF1B1B18), // #1B1B18
                onSurface = Color(0xFF1B1B18) // #1B1B18
        )

private val DarkColors =
        darkColorScheme(
                primary = Color(0xFFdd7f40), // #dd7f40
                secondary = Color(0xFFdd7f40), // #dd7f40
                tertiary = Color(0xFFE5B59C), // #E5B59C
                background = Color(0xFF121413), // #121413
                surface = Color(0xFF1C1F1E), // #1C1F1E
                onPrimary = Color(0xFF00363B), // #00363B
                onBackground = Color(0xFFE5E5E0), // #E5E5E0
                onSurface = Color(0xFFE5E5E0) // #E5E5E0
        )

enum class AppThemeMode(val label: String) {
    FollowSystem("Follow System"),
    Dark("Dark"),
    Light("Light")
}

/** Metro-influenced Material 3 theme foundation. */
@Composable
fun LocalMusicTheme(
        themeMode: AppThemeMode = AppThemeMode.FollowSystem,
        content: @Composable () -> Unit
) {
    val useDarkColors =
            when (themeMode) {
                AppThemeMode.FollowSystem -> isSystemInDarkTheme()
                AppThemeMode.Dark -> true
                AppThemeMode.Light -> false
            }

    MaterialTheme(
            colorScheme = if (useDarkColors) DarkColors else LightColors,
            typography = MaterialTheme.typography,
            content = content
    )
}
