package com.mzagorscak.spiza.ui.components.animatable.animatedicons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathFillType
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

private const val ANIMATION_DURATION_MS = 450

// The shackle lifts out of the body first, then flips over around its right leg while the whole lock shifts left,
// so the open lock stays centred.
private const val LIFT_END = 0.4f
private const val FLIP_START = 0.3f
private const val SHACKLE_LIFT = 2f
private const val SHACKLE_PIVOT_X = 16f
private const val OPEN_SHIFT_X = 3f

private val BodyPath =
    addPathNodes(
        "M7,10H17A2,2 0,0 1,19 12V20A2,2 0,0 1,17 22H7A2,2 0,0 1,5 20V12A2,2 0,0 1,7 10Z" +
            "M7,12H17V20H7Z" +
            "M12,14A2,2 0,1 1,12 18A2,2 0,1 1,12 14Z",
    )

// Joins the pivot leg to the body once the shackle is lifted.
private val PivotStubPath = addPathNodes("M15,8H17V11H15Z")

private val ShacklePath = addPathNodes("M7,10.5V7A5,5 0,0 1,17 7V10.5H15V7A3,3 0,0 0,9 7V10.5Z")

/** A padlock that animates between closed ([isLocked]) and open; tint it like any other icon. */
@Composable
fun rememberAnimatedLockPainter(isLocked: Boolean): Painter =
    rememberAnimatedIconPainter(
        atEnd = isLocked.not(),
        label = "lockProgress",
        durationMillis = ANIMATION_DURATION_MS,
    ) { progress ->
        val lift = animationPhase(progress = progress, start = 0f, end = LIFT_END)
        val flip = animationPhase(progress = progress, start = FLIP_START, end = 1f)
        Group(translationX = -OPEN_SHIFT_X * flip) {
            Path(pathData = BodyPath, pathFillType = PathFillType.EvenOdd, fill = AnimatedIconFill)
            Path(pathData = PivotStubPath, fill = AnimatedIconFill)
            Group(translationY = -SHACKLE_LIFT * lift) {
                Group(scaleX = 1f - 2f * flip, pivotX = SHACKLE_PIVOT_X) {
                    Path(pathData = ShacklePath, fill = AnimatedIconFill)
                }
            }
        }
    }

@Composable
private fun AnimatedLockIconLayoutPreview() {
    var isLocked by remember { mutableStateOf(true) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(padding050),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(padding200),
    ) {
        IconButton(
            painter = rememberAnimatedLockPainter(isLocked = isLocked),
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Large,
            ),
            onClick = { isLocked = isLocked.not() },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AnimatedLockIconPreviewLightMode() {
    TheAppTheme {
        AnimatedLockIconLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AnimatedLockIconPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        AnimatedLockIconLayoutPreview()
    }
}
