package com.localmusic.player.ui.home

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SongSource
import com.localmusic.player.ui.theme.UiAnimationTimings
import kotlin.math.abs
import kotlin.math.roundToInt

private const val COVER_TRANSITION_ROTATION_DEGREES = 20f

private enum class ArtworkDragAxis {
    Horizontal,
    Vertical
}

private enum class CoverTransitionDirection {
    Previous,
    Next
}

private data class NowPlayingCover(
    val songId: String,
    val artworkUri: String?,
    val visualizerLevels: List<Float>,
    val isPlaying: Boolean
)

@Composable
internal fun AnimatedNowPlayingArtwork(
    song: Song,
    artworkUri: String?,
    visualizerLevels: List<Float>,
    isPlaying: Boolean,
    preferVisualizer: Boolean,
    isPreviousTransition: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFavouriteToggle: () -> Unit,
    onShowMoreActions: () -> Unit,
    onVisualizerEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maximumVolume = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var currentVolume by remember(audioManager) {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    val currentCover =
        NowPlayingCover(
            songId = song.id,
            artworkUri = artworkUri,
            visualizerLevels = visualizerLevels,
            isPlaying = isPlaying
        )
    var displayedCover by remember { mutableStateOf(currentCover) }
    var outgoingCover by remember { mutableStateOf<NowPlayingCover?>(null) }
    var activeDirection by remember {
        mutableStateOf(
            if (isPreviousTransition) {
                CoverTransitionDirection.Previous
            } else {
                CoverTransitionDirection.Next
            }
        )
    }
    val transitionProgress = remember { Animatable(1f) }
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val renderedCover =
        if (displayedCover.songId == currentCover.songId) currentCover else displayedCover

    DisposableEffect(context, audioManager) {
        val volumeObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                }
            }
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver
        )
        onDispose { context.contentResolver.unregisterContentObserver(volumeObserver) }
    }

    LaunchedEffect(currentCover.songId) {
        if (displayedCover.songId == currentCover.songId) {
            displayedCover = currentCover
            return@LaunchedEffect
        }

        outgoingCover = displayedCover
        displayedCover = currentCover
        activeDirection =
            if (isPreviousTransition) {
                CoverTransitionDirection.Previous
            } else {
                CoverTransitionDirection.Next
            }
        transitionProgress.snapTo(0f)
        transitionProgress.animateTo(
            1f,
            animationSpec = tween(durationMillis = UiAnimationTimings.COVER_TRANSITION_MILLIS)
        )
        outgoingCover = null
    }

    BoxWithConstraints(
        modifier =
            modifier.clipToBounds().pointerInput(song.id, maximumVolume) {
                var horizontalDrag = 0f
                var verticalDrag = 0f
                var startingVolume = currentVolume
                var dragAxis: ArtworkDragAxis? = null
                var actionTriggered = false
                detectDragGestures(
                    onDragStart = {
                        horizontalDrag = 0f
                        verticalDrag = 0f
                        startingVolume = currentVolume
                        dragAxis = null
                        actionTriggered = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        horizontalDrag += dragAmount.x
                        verticalDrag += dragAmount.y
                        if (dragAxis == null) {
                            dragAxis =
                                if (abs(horizontalDrag) >= abs(verticalDrag)) {
                                    ArtworkDragAxis.Horizontal
                                } else {
                                    ArtworkDragAxis.Vertical
                                }
                        }

                        when (dragAxis) {
                            ArtworkDragAxis.Horizontal -> {
                                if (!actionTriggered && horizontalDrag >= swipeThreshold) {
                                    onPrevious()
                                    actionTriggered = true
                                } else if (!actionTriggered && horizontalDrag <= -swipeThreshold) {
                                    onNext()
                                    actionTriggered = true
                                }
                            }

                            ArtworkDragAxis.Vertical -> {
                                val requestedVolume =
                                    volumeForVerticalDrag(
                                        startingVolume = startingVolume,
                                        maximumVolume = maximumVolume,
                                        dragDistance = verticalDrag,
                                        dragRange = size.height
                                    )
                                if (requestedVolume != currentVolume) {
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        requestedVolume,
                                        0
                                    )
                                    currentVolume = requestedVolume
                                }
                            }

                            null -> Unit
                        }
                    }
                )
            }
    ) {
        val coverWidth = with(LocalDensity.current) { maxWidth.toPx() }
        val progress = transitionProgress.value
        val outgoingTranslation =
            if (activeDirection == CoverTransitionDirection.Previous) coverWidth * progress
            else -coverWidth * progress
        val outgoingRotation =
            if (activeDirection == CoverTransitionDirection.Previous) {
                COVER_TRANSITION_ROTATION_DEGREES * progress
            } else {
                -COVER_TRANSITION_ROTATION_DEGREES * progress
            }
        val incomingRotationStart =
            if (activeDirection == CoverTransitionDirection.Previous) {
                -COVER_TRANSITION_ROTATION_DEGREES
            } else {
                COVER_TRANSITION_ROTATION_DEGREES
            }
        val incomingTranslationStart =
            if (activeDirection == CoverTransitionDirection.Previous) -coverWidth
            else coverWidth

        outgoingCover?.let { cover ->
            NowPlayingCoverArtwork(
                cover = cover,
                modifier =
                    Modifier.fillMaxSize().graphicsLayer {
                        translationX = outgoingTranslation
                        rotationZ = outgoingRotation
                    }
            )
        }
        NowPlayingCoverArtwork(
            cover = renderedCover,
            preferVisualizer = preferVisualizer,
            allowVisualizerToggle = outgoingCover == null,
            onVisualizerEnabledChange = onVisualizerEnabledChange,
            modifier =
                Modifier.fillMaxSize().graphicsLayer {
                    translationX = incomingTranslationStart * (1f - progress)
                    rotationZ = incomingRotationStart * (1f - progress)
                }
        )
        Column(
            modifier =
                Modifier.align(Alignment.BottomStart)
                    .padding(12.dp)
                    .zIndex(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text =
                    listOf(song.artist, song.album)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
        Row(
            modifier =
                Modifier.align(Alignment.TopEnd)
                    .padding(8.dp)
                    .zIndex(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        shape = MaterialTheme.shapes.medium
                    )
        ) {
            IconButton(
                onClick = onFavouriteToggle,
                modifier = Modifier.semantics {
                    contentDescription =
                        if (song.isFavourite) "Remove from favourites"
                        else "Add to favourites"
                }
            ) {
                Icon(
                    imageVector =
                        if (song.isFavourite) Icons.Filled.Favorite
                        else Icons.Outlined.FavoriteBorder,
                    contentDescription = null
                )
            }
            if (song.isCastEligible()) {
                AndroidView(
                    factory = { viewContext ->
                        MediaRouteButton(viewContext).also { button ->
                            CastButtonFactory.setUpMediaRouteButton(viewContext, button)
                            button.contentDescription = "Stream to device"
                        }
                    },
                    modifier = Modifier.size(48.dp)
                )
            }
            IconButton(
                onClick = onShowMoreActions,
                modifier = Modifier.semantics { contentDescription = "More actions" }
            ) {
                Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
            }
        }
        VolumeIndicator(
            volume = currentVolume,
            maximumVolume = maximumVolume,
            modifier =
                Modifier.align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .zIndex(1f)
        )
    }
}

private fun Song.isCastEligible(): Boolean =
    source == SongSource.STREAM &&
            runCatching {
                android.net.Uri.parse(uri).scheme?.lowercase() in setOf("http", "https")
            }.getOrDefault(false)

@Composable
private fun VolumeIndicator(
    volume: Int,
    maximumVolume: Int,
    modifier: Modifier = Modifier
) {
    val volumeFraction = volume.toFloat() / maximumVolume.coerceAtLeast(1)
    val shape = MaterialTheme.shapes.extraSmall
    Box(
        modifier =
            modifier.size(width = 8.dp, height = 120.dp)
                .clip(shape)
                .background(Color.DarkGray.copy(alpha = 0.5f))
                .semantics {
                    contentDescription = "Media volume"
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(volume.toFloat(), 0f..maximumVolume.toFloat())
                }
    ) {
        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = volumeFraction)
                    .background(Color.LightGray)
        )
    }
}

internal fun volumeForVerticalDrag(
    startingVolume: Int,
    maximumVolume: Int,
    dragDistance: Float,
    dragRange: Int
): Int {
    val volumeDelta = (-dragDistance / dragRange.coerceAtLeast(1)) * maximumVolume
    return (startingVolume + volumeDelta.roundToInt()).coerceIn(0, maximumVolume)
}

@Composable
private fun NowPlayingCoverArtwork(
    cover: NowPlayingCover,
    modifier: Modifier,
    preferVisualizer: Boolean = false,
    allowVisualizerToggle: Boolean = false,
    onVisualizerEnabledChange: (Boolean) -> Unit = {}
) {
    ArtworkThumbnail(
        artworkUri = cover.artworkUri,
        visualizerLevels = cover.visualizerLevels,
        isPlaying = cover.isPlaying,
        preferVisualizer = preferVisualizer,
        placeholderGlyph = NO_ARTWORK_GLYPH,
        allowVisualizerToggle = allowVisualizerToggle,
        onVisualizerEnabledChange = onVisualizerEnabledChange,
        modifier = modifier
    )
}
