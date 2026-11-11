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
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

// Same shapes and viewport as ic_trash, split into the bin and its lid so the lid can move on its own.
private const val VIEWPORT_SIZE = 32f
private const val ANIMATION_DURATION_MS = 500

// The lid lifts straight up above the can's top edge and drops back.
private const val LID_LIFT = 4f

private val BinPath =
    addPathNodes(
        "M10,15C10,14.448 10.448,14 11,14C11.552,14 12,14.448 12,15L12,27C12,27.553 11.552,28 11,28" +
            "C10.448,28 10,27.553 10,27L10,15ZM15,15C15,14.448 15.448,14 16,14C16.552,14 17,14.448 17,15L17,27" +
            "C17,27.553 16.552,28 16,28C15.448,28 15,27.553 15,27L15,15ZM20,15C20,14.448 20.448,14 21,14" +
            "C21.552,14 22,14.448 22,15L22,27C22,27.553 21.552,28 21,28C20.448,28 20,27.553 20,27L20,15Z" +
            "M5,28C5,30.209 6.791,32 9,32L23,32C25.209,32 27,30.209 27,28L27,12L5,12L5,28Z",
    )

private val LidPath =
    addPathNodes(
        "M19,4L13,4L13,3C13,2.447 13.448,2 14,2L18,2C18.552,2 19,2.447 19,3L19,4Z" +
            "M27,4L21,4L21,2C21,0.896 20.104,0 19,0L13,0C11.896,0 11,0.896 11,2L11,4L5,4C3.896,4 3,4.896 3,6L3,8" +
            "C3,9.104 3.895,9.999 4.999,10L27.002,10C28.105,9.999 29,9.104 29,8L29,6C29,4.896 28.104,4 27,4Z",
    )

/** A trash can whose lid lifts and drops back every time [clicks] grows; count the clicks into it. */
@Composable
fun rememberAnimatedTrashPainter(clicks: Int): Painter =
    rememberAnimatedIconPainter(
        targetProgress = clicks.toFloat(),
        label = "trashClicks",
        viewportSize = VIEWPORT_SIZE,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS, easing = LinearEasing),
        headroom = LID_LIFT,
    ) { progress ->
        // Every click runs one whole step of the progress; the sine lifts the lid in its first half and drops it in the second.
        val lift = sin(PI.toFloat() * (progress - floor(progress)))
        Path(pathData = BinPath, pathFillType = PathFillType.EvenOdd, fill = AnimatedIconFill)
        Group(translationY = -LID_LIFT * lift) {
            Path(pathData = LidPath, pathFillType = PathFillType.EvenOdd, fill = AnimatedIconFill)
        }
    }

@Composable
private fun AnimatedTrashIconLayoutPreview() {
    var clicks by remember { mutableIntStateOf(0) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(padding050),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(padding200),
    ) {
        IconButton(
            painter = rememberAnimatedTrashPainter(clicks = clicks),
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
private fun AnimatedTrashIconPreviewLightMode() {
    TheAppTheme {
        AnimatedTrashIconLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AnimatedTrashIconPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        AnimatedTrashIconLayoutPreview()
    }
}
