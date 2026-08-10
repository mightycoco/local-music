package com.localmusic.player.ui.home

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.localmusic.player.ui.theme.UiAnimationTimings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ArtworkThumbnail(
        artworkUri: String?,
        modifier: Modifier = Modifier.size(48.dp),
        visualizerLevels: List<Float> = emptyList(),
        isPlaying: Boolean = false,
        preferVisualizer: Boolean = false,
        placeholderGlyph: Glyphs = Glyphs.NO_ARTWORK_THUMB,
        allowVisualizerToggle: Boolean = false,
        onVisualizerEnabledChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isArtworkLoading by remember { mutableStateOf(artworkUri != null) }
    var showVisualizer by remember { mutableStateOf(preferVisualizer || artworkUri == null) }

    LaunchedEffect(artworkUri) {
        isArtworkLoading = artworkUri != null
        val decodedBitmap =
                artworkUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    }
                }
        bitmap = decodedBitmap
        showVisualizer = preferVisualizer || decodedBitmap == null
        isArtworkLoading = false
    }

    LaunchedEffect(allowVisualizerToggle, isArtworkLoading, showVisualizer) {
        if (allowVisualizerToggle && !isArtworkLoading) {
            onVisualizerEnabledChange(showVisualizer)
        }
    }

    LaunchedEffect(preferVisualizer) { if (preferVisualizer) showVisualizer = true }

    val thumbnail = bitmap
    val canShowVisualizer = allowVisualizerToggle && showVisualizer
    val toggleModifier =
            if (allowVisualizerToggle && !isArtworkLoading) {
                Modifier.clickable { showVisualizer = !showVisualizer }
            } else {
                Modifier
            }
    AnimatedContent(
            targetState = canShowVisualizer to thumbnail,
            modifier = modifier.then(toggleModifier),
            transitionSpec = {
                (fadeIn(animationSpec = tween(UiAnimationTimings.ARTWORK_VISUALIZER_ENTER_MILLIS)) +
                                scaleIn(initialScale = 0.96f))
                        .togetherWith(
                                fadeOut(
                                        animationSpec =
                                                tween(
                                                        UiAnimationTimings
                                                                .ARTWORK_VISUALIZER_EXIT_MILLIS
                                                )
                                ) + scaleOut(targetScale = 1.04f)
                        )
            },
            label = "artworkVisualizerToggle"
    ) { (showVisualizer, displayedBitmap) ->
        if (showVisualizer) {
            NoArtworkVisualizer(
                    levels = visualizerLevels,
                    isPlaying = isPlaying,
                    placeholderGlyph = placeholderGlyph,
                    modifier = Modifier.fillMaxSize()
            )
        } else if (displayedBitmap != null) {
            Image(
                    bitmap = displayedBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
            )
        } else {
            ArtworkPlaceholder(glyph = placeholderGlyph, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ArtworkPlaceholder(glyph: Glyphs, modifier: Modifier) {
    Box(
            modifier =
                    modifier.background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                            shape = MaterialTheme.shapes.large
                    ),
            contentAlignment = Alignment.Center
    ) { Text(text = glyph.glyph, style = MaterialTheme.typography.displayMedium) }
}

@Composable
private fun NoArtworkVisualizer(
        levels: List<Float>,
        isPlaying: Boolean,
        placeholderGlyph: Glyphs,
        modifier: Modifier
) {
    val displayLevels = if (levels.isEmpty()) List(9) { 0f } else levels
    Box(
            modifier =
                    modifier.background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                            shape = MaterialTheme.shapes.large
                    ),
            contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            val colorTransition = rememberInfiniteTransition(label = "visualizerColors")
            val colorProgress by
                    colorTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec =
                                    infiniteRepeatable(
                                            animation =
                                                    tween(
                                                            durationMillis =
                                                                    UiAnimationTimings
                                                                            .VISUALIZER_COLOR_CYCLE_MILLIS,
                                                            easing = LinearEasing
                                                    )
                                    ),
                            label = "visualizerColorProgress"
                    )
            val visualizerColor = visualizerColorAt(colorProgress)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val barHeights =
                        displayLevels.map { level ->
                            (maxHeight.value * (0.16f + 0.68f * level.coerceIn(0f, 1f))).dp
                        }
                VisualizerBars(
                        heights = barHeights,
                        color = visualizerColor,
                        modifier = Modifier.fillMaxSize().alpha(0.88f).blur(radius = 16.dp)
                )
                VisualizerBars(
                        heights = barHeights,
                        color = visualizerColor,
                        modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            ArtworkPlaceholder(glyph = placeholderGlyph, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun VisualizerBars(heights: List<Dp>, color: Color, modifier: Modifier) {
    Row(
            modifier = modifier.fillMaxWidth(0.82f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { height ->
            Box(
                    modifier =
                            Modifier.weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .height(height)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(color)
            )
        }
    }
}

private fun visualizerColorAt(progress: Float): Color {
    val palette = listOf(Color(0xFF00B8A9), Color(0xFFFF6B6B), Color(0xFFFFC857), Color(0xFF00B8A9))
    val position = progress.coerceIn(0f, 1f) * (palette.size - 1)
    val index = position.toInt().coerceAtMost(palette.lastIndex - 1)
    return lerp(palette[index], palette[index + 1], position - index)
}

@Composable
internal fun ArtworkBackdrop(artworkUri: String?) {
    val context = LocalContext.current
    var bitmap by remember(artworkUri) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(artworkUri) {
        bitmap =
                artworkUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        context.contentResolver
                                .openInputStream(Uri.parse(uri))
                                ?.use(BitmapFactory::decodeStream)
                    }
                }
    }

    bitmap?.let { artwork ->
        Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.16f),
                contentScale = ContentScale.Crop
        )
    }
}
