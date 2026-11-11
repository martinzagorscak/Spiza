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
import kotlin.math.sin

// The fill grows out of the pin's head while the whole pin dips down and back, as if it was pushed in.
private const val PRESS_DEPTH = 1.5f
private const val FILL_START_SCALE = 0.4f
private const val FILL_PIVOT_X = 12f
private const val FILL_PIVOT_Y = 8f

// The filled pin has the same outline as the outlined one, so at the end it covers it completely.
private val OutlinedPinPath =
    addPathNodes(
        "M14,4v5c0,1.12 0.37,2.16 1,3H9c0.65,-0.86 1,-1.9 1,-3V4H14M17,2H7C6.45,2 6,2.45 6,3c0,0.55 0.45,1 1,1l1,0v5" +
            "c0,1.66 -1.34,3 -3,3v2h5.97v7l1,1l1,-1v-7H19v-2c-1.66,0 -3,-1.34 -3,-3V4l1,0c0.55,0 1,-0.45 1,-1" +
            "C18,2.45 17.55,2 17,2L17,2z",
    )

private val FilledPinPath =
    addPathNodes(
        "M16,9V4l1,0c0.55,0 1,-0.45 1,-1c0,-0.55 -0.45,-1 -1,-1H7C6.45,2 6,2.45 6,3c0,0.55 0.45,1 1,1l1,0v5" +
            "c0,1.66 -1.34,3 -3,3v2h5.97v7l1,1l1,-1v-7H19v-2C17.34,12 16,10.66 16,9z",
    )

/** A pin that fills in when [isPinned] and empties when not; tint it like any other icon. */
@Composable
fun rememberAnimatedPinPainter(isPinned: Boolean): Painter =
    rememberAnimatedIconPainter(
        atEnd = isPinned,
        label = "pinProgress",
    ) { progress ->
        val fillScale = FILL_START_SCALE + (1f - FILL_START_SCALE) * progress
        Group(translationY = PRESS_DEPTH * sin(PI * progress).toFloat()) {
            Path(pathData = OutlinedPinPath, fill = AnimatedIconFill)
            Group(scaleX = fillScale, scaleY = fillScale, pivotX = FILL_PIVOT_X, pivotY = FILL_PIVOT_Y) {
                Path(pathData = FilledPinPath, fill = AnimatedIconFill, fillAlpha = progress)
            }
        }
    }

@Composable
private fun AnimatedPinIconLayoutPreview() {
    var isPinned by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(padding050),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(padding200),
    ) {
        IconButton(
            painter = rememberAnimatedPinPainter(isPinned = isPinned),
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Large,
            ),
            onClick = { isPinned = isPinned.not() },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AnimatedPinIconPreviewLightMode() {
    TheAppTheme {
        AnimatedPinIconLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AnimatedPinIconPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        AnimatedPinIconLayoutPreview()
    }
}
