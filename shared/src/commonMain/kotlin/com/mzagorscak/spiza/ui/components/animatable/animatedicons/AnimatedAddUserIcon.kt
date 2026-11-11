package com.mzagorscak.spiza.ui.components.animatable.animatedicons

import androidx.compose.animation.core.LinearEasing
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
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

// Same shapes and viewport as ic_add_user, split into the person and the plus so the plus can move on its own.
private const val ANIMATION_DURATION_MS = 500
private const val STROKE_WIDTH = 2f

// The plus spins a quarter turn, which ends on a pose identical to the resting one, and pops a little bigger halfway.
private const val PLUS_CENTER_X = 19f
private const val PLUS_CENTER_Y = 7f
private const val PLUS_ROTATION_DEGREES = 90f
private const val PLUS_POP_SCALE = 0.35f

private val PersonPath =
    addPathNodes(
        "M8,14H10C13.314,14 16,16.686 16,20C16,20.552 15.552,21 15,21H3C2.448,21 2,20.552 2,20C2,16.686 4.686,14 8,14Z" +
            "M9,4C10.657,4 12,5.343 12,7C12,8.657 10.657,10 9,10C7.343,10 6,8.657 6,7C6,5.343 7.343,4 9,4Z",
    )

private val PlusPath = addPathNodes("M19,4V10M16,7H22")

/** A person with a plus that spins and pops every time [clicks] grows; count the clicks into it. */
@Composable
fun rememberAnimatedAddUserPainter(clicks: Int): Painter =
    rememberAnimatedIconPainter(
        targetProgress = clicks.toFloat(),
        label = "addUserClicks",
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS, easing = LinearEasing),
    ) { progress ->
        // Every click runs one whole step of the progress: the plus turns with it and swells in the middle of the step.
        val phase = progress - floor(progress)
        val scale = 1f + PLUS_POP_SCALE * sin(PI.toFloat() * phase)
        Path(
            pathData = PersonPath,
            fill = AnimatedIconFill,
            stroke = AnimatedIconFill,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        Group(
            rotation = PLUS_ROTATION_DEGREES * progress,
            pivotX = PLUS_CENTER_X,
            pivotY = PLUS_CENTER_Y,
            scaleX = scale,
            scaleY = scale,
        ) {
            Path(
                pathData = PlusPath,
                stroke = AnimatedIconFill,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }

@Composable
private fun AnimatedAddUserIconLayoutPreview() {
    var clicks by remember { mutableIntStateOf(0) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(padding050),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(padding200),
    ) {
        IconButton(
            painter = rememberAnimatedAddUserPainter(clicks = clicks),
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Large,
            ),
            onClick = { clicks++ },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AnimatedAddUserIconPreviewLightMode() {
    TheAppTheme {
        AnimatedAddUserIconLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AnimatedAddUserIconPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        AnimatedAddUserIconLayoutPreview()
    }
}
