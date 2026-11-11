package com.mzagorscak.spiza.ui.components.animatable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.hypot
import kotlin.math.max

private const val DEFAULT_DURATION_MS = 500

/**
 * Shows [content] for [targetState]; when it changes, the old frame is captured and kept on top while the new state is
 * revealed through a circle growing from the last touch (or the centre when nothing was touched).
 * Only the draw phase runs during the animation, nothing recomposes. Dialogs and popups live in their own windows,
 * so they are not captured and switch straight away.
 */
@Composable
fun <T> CircularRevealTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    durationMillis: Int = DEFAULT_DURATION_MS,
    content: @Composable (state: T) -> Unit,
) {
    // Lags [targetState] until the old frame has been captured.
    var shownState by remember { mutableStateOf(targetState) }
    var oldStateSnapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var revealCenter by remember { mutableStateOf(Offset.Unspecified) }
    var lastPressPosition by remember { mutableStateOf(Offset.Unspecified) }
    val revealProgress = remember { Animatable(0f) }
    val contentLayer = rememberGraphicsLayer()

    LaunchedEffect(targetState) {
        if (targetState == shownState) return@LaunchedEffect
        oldStateSnapshot = contentLayer.toImageBitmap()
        revealCenter = lastPressPosition
        shownState = targetState
        revealProgress.snapTo(0f)
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        )
        oldStateSnapshot = null
    }

    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .trackPresses { lastPressPosition = it }
            .drawWithContent {
                contentLayer.record { this@drawWithContent.drawContent() }
                drawLayer(contentLayer)
                oldStateSnapshot?.let { snapshot ->
                    val origin = revealCenter.takeIf { it.isSpecified } ?: center
                    val radius = revealProgress.value * maxDistanceToCorner(origin)
                    val revealedArea = Path().apply { addOval(Rect(center = origin, radius = radius)) }
                    clipPath(path = revealedArea, clipOp = ClipOp.Difference) {
                        drawImage(snapshot)
                    }
                }
            },
    ) {
        content(shownState)
    }
}

/** Reports where each press starts, without consuming it, so the content below still gets every touch. */
private fun Modifier.trackPresses(onPress: (Offset) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                    .changes
                    .firstOrNull { it.pressed && it.previousPressed.not() }
                    ?.let { onPress(it.position) }
            }
        }
    }

/** Radius at which a circle around [origin] covers the whole draw area. */
private fun DrawScope.maxDistanceToCorner(origin: Offset): Float {
    val dx = max(origin.x, size.width - origin.x)
    val dy = max(origin.y, size.height - origin.y)
    return hypot(dx, dy)
}
