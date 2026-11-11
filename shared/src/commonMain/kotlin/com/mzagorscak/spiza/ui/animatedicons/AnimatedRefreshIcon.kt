package com.mzagorscak.spiza.ui.animatedicons

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding050
import com.mzagorscak.spiza.ui.theme.padding200

private const val ANIMATION_DURATION_MS = 600
private const val DEGREES_PER_TURN = 360f
private const val CENTER = 12f
private const val STROKE_WIDTH = 2f

// Drawn as a stroke, not a fill: two arcs with their arrow heads.
private val RefreshPath =
    addPathNodes(
        "M4.062,13C4.021,12.672 4,12.339 4,12C4,7.582 7.582,4 12,4C14.501,4 16.733,5.147 18.2,6.944" +
            "M19.938,11C19.979,11.328 20,11.661 20,12C20,16.418 16.418,20 12,20C9.611,20 7.466,18.952 6,17.292" +
            "M9,17H6V17.292M18.2,4V6.944M18.2,6.944V7L15.2,7M6,20V17.292",
    )

/** Refresh arrows that spin one full turn every time [turns] grows; count the clicks into it. */
@Composable
fun rememberAnimatedRefreshPainter(turns: Int): Painter =
    rememberAnimatedIconPainter(
        targetProgress = turns.toFloat(),
        label = "refreshTurns",
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
    ) { progress ->
        Group(rotation = DEGREES_PER_TURN * progress, pivotX = CENTER, pivotY = CENTER) {
            Path(
                pathData = RefreshPath,
                stroke = AnimatedIconFill,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }

@Composable
private fun AnimatedRefreshIconLayoutPreview() {
    var turns by remember { mutableIntStateOf(0) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(padding050),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(padding200),
    ) {
        IconButton(
            painter = rememberAnimatedRefreshPainter(turns = turns),
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Large,
            ),
            onClick = { turns++ },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AnimatedRefreshIconPreviewLightMode() {
    TheAppTheme {
        AnimatedRefreshIconLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AnimatedRefreshIconPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        AnimatedRefreshIconLayoutPreview()
    }
}
