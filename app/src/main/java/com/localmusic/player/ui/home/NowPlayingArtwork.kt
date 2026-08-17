package com.localmusic.player.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.localmusic.player.domain.model.Song
import com.localmusic.player.ui.theme.UiAnimationTimings

private const val COVER_TRANSITION_ROTATION_DEGREES = 20f

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
            modifier.clipToBounds().pointerInput(song.id) {
                var totalDrag = 0f
                var actionTriggered = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        actionTriggered = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (actionTriggered) {
                            return@detectHorizontalDragGestures
                        }

                        totalDrag += dragAmount
                        when {
                            totalDrag >= swipeThreshold -> {
                                onPrevious()
                                actionTriggered = true
                            }

                            totalDrag <= -swipeThreshold -> {
                                onNext()
                                actionTriggered = true
                            }
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
            IconButton(
                onClick = onShowMoreActions,
                modifier = Modifier.semantics { contentDescription = "More actions" }
            ) {
                Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
            }
        }
    }
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
