package com.mzagorscak.spiza.ui.animatedicons

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.VectorComposable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

private const val ICON_SIZE_DP = 24
private const val DEFAULT_VIEWPORT_SIZE = 24f
private const val DEFAULT_DURATION_MS = 300

/** Fill for animated icon paths; the real colour comes from the tint of the `Icon` / `IconButton` that draws the painter. */
internal val AnimatedIconFill: Brush = SolidColor(Color.Black)

/**
 * Base of every on/off animated icon: animates a progress from 0 (`atEnd = false`) to 1 (`atEnd = true`) and back, and
 * draws [content] with it into a vector painter. An icon maps the progress to the transforms of its `Group`s (rotation,
 * translation, scale) around fixed `Path`s, so the shapes never have to be morphed.
 */
@Composable
internal fun rememberAnimatedIconPainter(
    atEnd: Boolean,
    label: String,
    viewportSize: Float = DEFAULT_VIEWPORT_SIZE,
    durationMillis: Int = DEFAULT_DURATION_MS,
    // ktlint's formatter and checker disagree on a function type with two annotations, so the rules are suppressed here.
    @Suppress("ktlint:standard:annotation", "ktlint:standard:function-type-modifier-spacing")
    content: @Composable @VectorComposable (progress: Float) -> Unit,
): Painter =
    rememberAnimatedIconPainter(
        targetProgress = if (atEnd) 1f else 0f,
        label = label,
        viewportSize = viewportSize,
        animationSpec = tween(durationMillis = durationMillis),
        content = content,
    )

/**
 * Same as the on/off variant, but the progress follows any [targetProgress]: for icons that react to an event rather than
 * a state, e.g. one more turn of a gear per click ([targetProgress] = the click count).
 */
@Composable
internal fun rememberAnimatedIconPainter(
    targetProgress: Float,
    label: String,
    viewportSize: Float = DEFAULT_VIEWPORT_SIZE,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = DEFAULT_DURATION_MS),
    @Suppress("ktlint:standard:annotation", "ktlint:standard:function-type-modifier-spacing")
    content: @Composable @VectorComposable (progress: Float) -> Unit,
): Painter {
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = animationSpec,
        label = label,
    )
    return rememberVectorPainter(
        defaultWidth = ICON_SIZE_DP.dp,
        defaultHeight = ICON_SIZE_DP.dp,
        viewportWidth = viewportSize,
        viewportHeight = viewportSize,
        autoMirror = false,
    ) { _, _ ->
        content(progress)
    }
}

/**
 * Maps the overall [progress] to a phase running from 0 at [start] to 1 at [end], so one icon can stagger several moves
 * (e.g. lift first, then flip). Overlapping phases make the moves read as one gesture.
 */
internal fun animationPhase(
    progress: Float,
    start: Float,
    end: Float,
): Float = ((progress - start) / (end - start)).coerceIn(0f, 1f)
