package com.mzagorscak.spiza.ui.components.animatable.animatedicons

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.Group
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
 * a state, e.g. one more turn of a gear per click ([targetProgress] = the click count). With an [initialProgress] different
 * from [targetProgress] the icon animates as soon as it is composed.
 * [headroom] (in viewport units) lets parts of the icon move above its top edge, e.g. a lid that lifts off a full-height can;
 * the icon keeps its size and position.
 */
@Composable
internal fun rememberAnimatedIconPainter(
    targetProgress: Float,
    label: String,
    viewportSize: Float = DEFAULT_VIEWPORT_SIZE,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = DEFAULT_DURATION_MS),
    initialProgress: Float = targetProgress,
    headroom: Float = 0f,
    @Suppress("ktlint:standard:annotation", "ktlint:standard:function-type-modifier-spacing")
    content: @Composable @VectorComposable (progress: Float) -> Unit,
): Painter {
    // Starts at [initialProgress] and only then follows [targetProgress], so an icon can also animate when it first appears.
    var animatedTarget by remember { mutableFloatStateOf(initialProgress) }
    LaunchedEffect(targetProgress) { animatedTarget = targetProgress }
    val progress by animateFloatAsState(
        targetValue = animatedTarget,
        animationSpec = animationSpec,
        label = label,
    )
    val headroomFraction = headroom / viewportSize
    val vectorPainter = rememberVectorPainter(
        defaultWidth = ICON_SIZE_DP.dp,
        defaultHeight = (ICON_SIZE_DP * (1f + headroomFraction)).dp,
        viewportWidth = viewportSize,
        viewportHeight = viewportSize + headroom,
        autoMirror = false,
    ) { _, _ ->
        Group(translationY = headroom) {
            content(progress)
        }
    }
    if (headroom == 0f) return vectorPainter
    return remember(vectorPainter, headroomFraction) {
        HeadroomPainter(painter = vectorPainter, headroomFraction = headroomFraction)
    }
}

/**
 * Lays [painter] out as a square icon but draws its extra top [headroomFraction] above the bounds. A vector painter clips to
 * its own size, so the headroom has to be part of it; `Icon` does not clip, so the overflow lands in the button's padding.
 */
private class HeadroomPainter(
    private val painter: Painter,
    private val headroomFraction: Float,
) : Painter() {
    private var alpha: Float = DefaultAlpha
    private var colorFilter: ColorFilter? = null

    override val intrinsicSize: Size
        get() = Size(width = painter.intrinsicSize.width, height = painter.intrinsicSize.width)

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        val headroomHeight = size.height * headroomFraction
        translate(top = -headroomHeight) {
            with(painter) {
                draw(
                    size = Size(width = size.width, height = size.height + headroomHeight),
                    alpha = alpha,
                    colorFilter = colorFilter,
                )
            }
        }
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
